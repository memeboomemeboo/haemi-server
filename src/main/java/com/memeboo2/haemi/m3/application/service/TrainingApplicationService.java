package com.memeboo2.haemi.m3.application.service;

import com.memeboo2.haemi.m0.domain.port.ElderMembershipQuery;
import com.memeboo2.haemi.m0.domain.port.ElderStatusQuery;
import com.memeboo2.haemi.m1.application.service.AlbumNotFoundException;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m3.application.command.*;
import com.memeboo2.haemi.m3.application.dto.AccruedHintResult;
import com.memeboo2.haemi.m3.application.dto.AnswerResult;
import com.memeboo2.haemi.m3.application.dto.ChanceResult;
import com.memeboo2.haemi.m3.application.dto.HintServeResult;
import com.memeboo2.haemi.m3.application.dto.TrainingSessionResult;
import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import com.memeboo2.haemi.m3.domain.model.hint.HintBankResolver;
import com.memeboo2.haemi.m3.domain.model.hint.ResolvedHint;
import com.memeboo2.haemi.m3.domain.repository.AccruedHintRepository;
import com.memeboo2.haemi.m3.application.query.GetTodayTrainingSessionQuery;
import com.memeboo2.haemi.m3.domain.event.DifficultyLevelChangedEvent;
import com.memeboo2.haemi.m3.domain.model.training.*;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import com.memeboo2.haemi.m3.domain.port.OnlineFamilyMemberQuery;
import com.memeboo2.haemi.m3.domain.port.HintPlaybackSafetyQuery;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingApplicationService {

    private final TrainingSessionRepository sessionRepository;
    private final DifficultyProfileRepository profileRepository;
    private final DifficultyPolicyRepository policyRepository;
    private final AlbumRepository albumRepository;
    private final AccruedHintRepository accruedHintRepository;
    private final CognitiveQuestionGeneratorPort questionGeneratorPort;
    private final TrainingSpeechSynthesisPort speechSynthesisPort;
    private final ApplicationEventPublisher eventPublisher;
    private final ElderStatusQuery elderStatusQuery;
    private final ElderMembershipQuery elderMembershipQuery;
    private final OnlineFamilyMemberQuery onlineFamilyMembers;
    private final HintPlaybackSafetyQuery hintPlaybackSafety;

    private static final int MIN_PHOTO_COUNT = 5;

    // F3-01: 일일 인지 훈련 세션 시작
    @Transactional
    public TrainingSessionResult startSession(StartTrainingSessionCommand command) {
        // EX-F301-10: 사별/입원 등 부적합 상태면 세션 개시를 차단한다
        if (!elderStatusQuery.isDispatchable(command.elderId())) {
            throw new SessionStartBlockedByElderStatusException(command.elderId());
        }
        var todaySession = sessionRepository.findByElderIdAndSessionDate(command.elderId(), LocalDate.now());
        if (todaySession.isPresent()) {
            CognitiveTrainingSession existing = todaySession.get();
            if (existing.getStatus() == TrainingSessionStatus.COMPLETED) {
                throw new DailySessionAlreadyCompletedException(command.elderId());
            }
            if (existing.refreshGrandchildChanceStatus(LocalDateTime.now())) {
                sessionRepository.save(existing);
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
    @Transactional
    public TrainingSessionResult getTodaySession(GetTodayTrainingSessionQuery query) {
        CognitiveTrainingSession session = sessionRepository
                .findByElderIdAndSessionDate(query.elderId(), LocalDate.now())
                .orElseThrow(() -> new TrainingSessionNotFoundException(query.elderId()));
        if (session.refreshGrandchildChanceStatus(LocalDateTime.now())) {
            sessionRepository.save(session);
        }
        return toResult(session);
    }

    // F3-01/F3-02: 답안 제출 및 완료 시 난이도 갱신
    @Transactional
    public AnswerResult answerQuestion(AnswerTrainingQuestionCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        requireSessionDispatchable(session);
        QuestionAttempt attempt = session.answer(
                command.questionId(), command.voiceDetected(), command.vadDurationMs());

        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            adjustDifficulty(session);
        }

        sessionRepository.save(session);
        String message = completionMessage(session, attempt);
        return new AnswerResult(toResult(session), attempt.isResponded(), message);
    }

    // F3-03: 손주 한마디 사전 적립 (메모/온보딩/주간리마인더/반응 유도 4개 경로)
    @Transactional
    public AccruedHintResult accrueHint(AccrueHintCommand command) {
        // 요청자가 해당 어르신의 가족 그룹 구성원인지 검증 (타 그룹 적립 차단)
        if (!elderMembershipQuery.isActiveGroupMember(command.elderId(), parseMemberId(command.authorMemberId()))) {
            throw new HintAccrualAccessDeniedException();
        }
        AccruedHint hint = AccruedHint.accrue(
                command.elderId(), command.photoId(), command.personName(), command.source(),
                command.authorMemberId(), command.authorName(), command.text());
        return AccruedHintResult.from(accruedHintRepository.save(hint));
    }

    // F3-03: 적립 힌트 즉시 제공 (L1 사진특정 → L3 시스템기본)
    @Transactional
    public HintServeResult serveGrandchildHint(ServeGrandchildHintCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        requireSessionDispatchable(session);
        var photoId = session.currentQuestion()
                .map(TrainingQuestion::getPhotoId)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        var l1 = photoId == null
                ? java.util.Optional.<AccruedHint>empty()
                : accruedHintRepository.findLatestReusableByPhoto(session.getElderId(), photoId, now.minusDays(14));
        var safeL1 = filterUnsafeHint(l1);
        ResolvedHint resolved = HintBankResolver.resolve(safeL1);
        session.serveAccruedHint(resolved.text(), resolved.responderName());
        safeL1.ifPresent(hint -> {
            hint.markServed(now);
            accruedHintRepository.save(hint);
        });
        sessionRepository.save(session);
        return new HintServeResult(toResult(session), resolved.tier(), resolved.text());
    }

    // F3-03 L2: 온라인 상태인 가족 한 명에게만 실시간 힌트를 요청하고 60초 동안 응답을 기다린다.
    @Transactional
    public ChanceResult requestGrandchildChance(RequestGrandchildChanceCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        requireSessionDispatchable(session);
        LocalDateTime now = LocalDateTime.now();
        if (session.isGrandchildChancePending() && !session.refreshGrandchildChanceStatus(now)) {
            return new ChanceResult(session.getId().toString(), "가족의 한마디를 기다리고 있어요.");
        }
        return onlineFamilyMembers.findOneOnlineMemberId(session.getElderId(), now.minusMinutes(2))
                .map(memberId -> {
                    session.requestGrandchildChance(java.util.Set.of(memberId.toString()));
                    sessionRepository.save(session);
                    return new ChanceResult(session.getId().toString(), "온라인 가족에게 한마디를 요청했어요.");
                })
                .orElseGet(() -> new ChanceResult(session.getId().toString(),
                        "온라인 가족이 없어 시스템 안내를 들려드려요."));
    }

    // F3-03: 가족 힌트 전달
    @Transactional
    public TrainingSessionResult provideHint(ProvideHintCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        Album album = albumRepository.findById(AlbumId.of(session.getAlbumId()))
                .orElseThrow(() -> new AlbumNotFoundException(session.getAlbumId().toString()));
        if (!album.isMember(command.responderMemberId())) {
            throw new GrandchildChanceResponderNotMemberException();
        }
        session.applyHint(command.responderMemberId(), command.responderName(), command.hintText());
        sessionRepository.save(session);
        return toResult(session);
    }

    // F3-01: 60초 무응답 허용 — 발화 없이 다음 사진으로 진행
    @Transactional
    public TrainingSessionResult recordNoResponse(RecordNoResponseCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        requireSessionDispatchable(session);
        session.recordNoResponse(command.questionId());
        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            adjustDifficulty(session);
        }
        sessionRepository.save(session);
        return toResult(session);
    }

    @Transactional
    public TrainingSessionResult passQuestion(PassTrainingQuestionCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        requireSessionDispatchable(session);
        session.passCurrentQuestion();
        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            adjustDifficulty(session);
        }
        sessionRepository.save(session);
        return toResult(session);
    }

    private static UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(memberId);
        } catch (IllegalArgumentException invalidUuid) {
            return null;
        }
    }

    private java.util.Optional<AccruedHint> filterUnsafeHint(java.util.Optional<AccruedHint> candidate) {
        if (candidate.isEmpty() || hintPlaybackSafety.isPlayable(
                candidate.get().getElderId(), candidate.get().getPhotoId(),
                candidate.get().getAuthorMemberId(), candidate.get().getPersonName())) {
            return candidate;
        }
        AccruedHint unsafe = candidate.get();
        unsafe.suppress();
        accruedHintRepository.save(unsafe);
        log.warn("숨김 또는 사별 인물과 관련된 힌트 재생을 차단했습니다: hintId={}", unsafe.getId());
        return java.util.Optional.empty();
    }

    private CognitiveTrainingSession loadSessionOrThrow(String sessionId) {
        return sessionRepository.findById(TrainingSessionId.of(sessionId))
                .orElseThrow(() -> new TrainingSessionNotFoundException(sessionId));
    }

    private void requireSessionDispatchable(CognitiveTrainingSession session) {
        if (elderStatusQuery.isDispatchable(session.getElderId())) {
            return;
        }
        session.abandonForElderStatus();
        sessionRepository.save(session);
        throw new SessionStartBlockedByElderStatusException(session.getElderId());
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
                session.getResponseRate(),
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
        return "오늘의 회상을 모두 마쳤습니다. 좋은 이야기 많이 들려주셔서 고맙습니다.";
    }

    private String completionMessage(CognitiveTrainingSession session, QuestionAttempt attempt) {
        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            return completedSpeech(session);
        }
        return attempt.isResponded()
                ? "이야기해 주셔서 고맙습니다. 다음 사진도 함께 볼까요?"
                : "괜찮아요, 다음 사진을 함께 볼까요?";
    }
}
