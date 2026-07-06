package com.memeboo2.haemi.m3.application.service;

import com.memeboo2.haemi.m1.application.service.AlbumNotFoundException;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m3.application.command.*;
import com.memeboo2.haemi.m3.application.dto.AnswerResult;
import com.memeboo2.haemi.m3.application.dto.ChanceResult;
import com.memeboo2.haemi.m3.application.dto.TrainingSessionResult;
import com.memeboo2.haemi.m3.application.query.GetTodayTrainingSessionQuery;
import com.memeboo2.haemi.m3.domain.event.DifficultyLevelChangedEvent;
import com.memeboo2.haemi.m3.domain.model.training.*;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import com.memeboo2.haemi.m3.domain.port.TrainingSpeechSynthesisPort;
import com.memeboo2.haemi.m3.domain.repository.DifficultyPolicyRepository;
import com.memeboo2.haemi.m3.domain.repository.DifficultyProfileRepository;
import com.memeboo2.haemi.m3.domain.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingApplicationService {

    private final TrainingSessionRepository sessionRepository;
    private final DifficultyProfileRepository profileRepository;
    private final DifficultyPolicyRepository policyRepository;
    private final AlbumRepository albumRepository;
    private final CognitiveQuestionGeneratorPort questionGeneratorPort;
    private final TrainingSpeechSynthesisPort speechSynthesisPort;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MIN_PHOTO_COUNT = 5;

    // F3-01: 일일 인지 훈련 세션 시작
    @Transactional
    public TrainingSessionResult startSession(StartTrainingSessionCommand command) {
        var todaySession = sessionRepository.findByElderIdAndSessionDate(command.elderId(), LocalDate.now());
        if (todaySession.isPresent()) {
            CognitiveTrainingSession existing = todaySession.get();
            if (existing.getStatus() == TrainingSessionStatus.COMPLETED) {
                throw new DailySessionAlreadyCompletedException(command.elderId());
            }
            return toResult(existing);
        }

        Album album = loadEligibleAlbum(command);
        DifficultyProfile profile = profileRepository.findByElderId(command.elderId())
                .orElseGet(() -> profileRepository.save(DifficultyProfile.defaultFor(command.elderId())));
        DifficultyPolicy policy = loadPolicy(profile.getCurrentLevel());
        List<TrainingQuestion> questions = generateQuestionsWithFallback(
                command.elderId(),
                album,
                profile.getCurrentLevel(),
                profile.recommendQuestionTypes(policy)
        );

        CognitiveTrainingSession session = CognitiveTrainingSession.start(
                command.elderId(), album.getId(),
                command.startMode(), profile.getCurrentLevel(), questions);
        sessionRepository.save(session);
        log.info("인지 훈련 세션 시작: elderId={}, sessionId={}", command.elderId(), session.getSessionId());
        return toResult(session);
    }

    // F3-01: 당일 이어서 풀기 조회
    @Transactional(readOnly = true)
    public TrainingSessionResult getTodaySession(GetTodayTrainingSessionQuery query) {
        CognitiveTrainingSession session = sessionRepository
                .findByElderIdAndSessionDate(query.elderId(), LocalDate.now())
                .orElseThrow(() -> new TrainingSessionNotFoundException(query.elderId()));
        return toResult(session);
    }

    // F3-01/F3-02: 답안 제출 및 완료 시 난이도 갱신
    @Transactional
    public AnswerResult answerQuestion(AnswerTrainingQuestionCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        QuestionAttempt attempt = session.answer(
                command.questionId(), command.submittedAnswer(), command.responseSeconds());

        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            adjustDifficulty(session);
        }

        sessionRepository.save(session);
        String message = completionMessage(session, attempt);
        return new AnswerResult(toResult(session), attempt.isCorrect(), message);
    }

    // F3-03: 손주 찬스 요청
    @Transactional
    public ChanceResult requestGrandchildChance(RequestGrandchildChanceCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        int remaining = session.requestGrandchildChance();
        sessionRepository.save(session);
        return new ChanceResult(command.sessionId(), remaining, "가족에게 힌트를 요청했습니다.");
    }

    // F3-03: 가족 힌트 전달
    @Transactional
    public TrainingSessionResult provideHint(ProvideHintCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        session.applyHint(command.responderName(), command.hintText());
        sessionRepository.save(session);
        return toResult(session);
    }

    private CognitiveTrainingSession loadSessionOrThrow(String sessionId) {
        return sessionRepository.findById(TrainingSessionId.of(sessionId))
                .orElseThrow(() -> new TrainingSessionNotFoundException(sessionId));
    }

    private Album loadEligibleAlbum(StartTrainingSessionCommand command) {
        AlbumId albumId = AlbumId.of(command.albumId());
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new AlbumNotFoundException(command.albumId()));
        if (!album.getElderProfileId().equals(command.elderId())) {
            throw TrainingPrerequisiteNotMetException.profileMismatch(
                    command.elderId(), album.getElderProfileId());
        }
        if (album.getPhotos().size() < MIN_PHOTO_COUNT) {
            throw TrainingPrerequisiteNotMetException.insufficientPhotos(
                    album.getPhotos().size(), MIN_PHOTO_COUNT);
        }
        return album;
    }

    private List<TrainingQuestion> generateQuestionsWithFallback(
            String elderId,
            Album album,
            int difficultyLevel,
            List<QuestionType> prioritizedQuestionTypes
    ) {
        try {
            return questionGeneratorPort.generate(
                    album,
                    difficultyLevel,
                    prioritizedQuestionTypes
            );
        } catch (RuntimeException generationFailure) {
            log.error(
                    "인지 훈련 문제 생성 실패, 이전 완료 세션 fallback 조회: elderId={}, albumId={}",
                    elderId,
                    album.getId(),
                    generationFailure
            );
            return sessionRepository.findLatestCompleted(elderId, album.getId())
                    .map(CognitiveTrainingSession::getQuestions)
                    .filter(cached -> !cached.isEmpty())
                    .map(cached -> cached.stream()
                            .map(TrainingQuestion::copyWithNewId)
                            .toList())
                    .orElseThrow(() -> new TrainingQuestionGenerationException(generationFailure));
        }
    }

    private void adjustDifficulty(CognitiveTrainingSession session) {
        DifficultyProfile profile = profileRepository.findByElderId(session.getElderId())
                .orElseGet(() -> DifficultyProfile.defaultFor(session.getElderId()));
        DifficultyPolicy policy = loadPolicy(profile.getCurrentLevel());
        DifficultyAdjustment adjustment = profile.applySession(
                session.getQuestionPerformances(),
                session.getAccuracyRate(),
                session.getAverageResponseSeconds(),
                policy
        );
        profileRepository.save(profile);

        if (adjustment.levelChanged()) {
            eventPublisher.publishEvent(new DifficultyLevelChangedEvent(
                    session.getId(),
                    session.getElderId(),
                    session.getAlbumId(),
                    adjustment.previousLevel(),
                    adjustment.currentLevel(),
                    adjustment.threeSessionMovingAverage(),
                    adjustment.repeatedWrongQuestionIds(),
                    LocalDateTime.now()
            ));
        }
    }

    private DifficultyPolicy loadPolicy(int level) {
        try {
            return policyRepository.findByLevel(level)
                    .orElseGet(() -> DifficultyPolicy.defaultFor(level));
        } catch (RuntimeException policyLoadFailure) {
            log.error("난이도 기준표 조회 실패, 기본 기준 적용: level={}", level, policyLoadFailure);
            return DifficultyPolicy.defaultFor(level);
        }
    }

    private TrainingSessionResult toResult(CognitiveTrainingSession session) {
        String speechText = session.getStatus() == TrainingSessionStatus.COMPLETED
                ? completedSpeech(session)
                : session.currentQuestion()
                        .map(TrainingQuestion::getPrompt)
                        .orElse("오늘의 인지 훈련을 시작해볼까요?");
        return TrainingSessionResult.from(session, speechSynthesisPort.synthesize(speechText));
    }

    private String completedSpeech(CognitiveTrainingSession session) {
        int accuracyPercent = (int) Math.round(session.getAccuracyRate() * 100);
        return "오늘의 훈련을 모두 마쳤습니다. 정말 잘하셨어요. 정답률은 %d퍼센트입니다."
                .formatted(accuracyPercent);
    }

    private String completionMessage(CognitiveTrainingSession session, QuestionAttempt attempt) {
        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            return completedSpeech(session);
        }
        return attempt.isCorrect() ? "잘하셨어요!" : "괜찮아요, 다음 문제를 풀어볼까요?";
    }
}
