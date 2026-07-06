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
import com.memeboo2.haemi.m3.domain.model.training.*;
import com.memeboo2.haemi.m3.domain.port.CognitiveQuestionGeneratorPort;
import com.memeboo2.haemi.m3.domain.repository.DifficultyProfileRepository;
import com.memeboo2.haemi.m3.domain.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingApplicationService {

    private final TrainingSessionRepository sessionRepository;
    private final DifficultyProfileRepository profileRepository;
    private final CognitiveQuestionGeneratorPort questionGeneratorPort;
    private final AlbumRepository albumRepository;

    // F3-01: 일일 인지 훈련 세션 시작
    @Transactional
    public TrainingSessionResult startSession(StartTrainingSessionCommand command) {
        var todaySession = sessionRepository.findByElderIdAndSessionDate(command.elderId(), LocalDate.now());
        if (todaySession.isPresent()) {
            CognitiveTrainingSession existing = todaySession.get();
            if (existing.getStatus() == TrainingSessionStatus.COMPLETED) {
                throw new DailySessionAlreadyCompletedException(command.elderId());
            }
            return TrainingSessionResult.from(existing);
        }

        DifficultyProfile profile = profileRepository.findByElderId(command.elderId())
                .orElseGet(() -> profileRepository.save(DifficultyProfile.defaultFor(command.elderId())));
        List<TrainingQuestion> questions = questionGeneratorPort.generate(
                command.elderId(), UUID.fromString(command.albumId()), profile.getCurrentLevel());

        CognitiveTrainingSession session = CognitiveTrainingSession.start(
                command.elderId(), UUID.fromString(command.albumId()),
                command.startMode(), profile.getCurrentLevel(), questions);
        sessionRepository.save(session);
        log.info("인지 훈련 세션 시작: elderId={}, sessionId={}", command.elderId(), session.getSessionId());
        return TrainingSessionResult.from(session);
    }

    // F3-01: 당일 이어서 풀기 조회
    @Transactional(readOnly = true)
    public TrainingSessionResult getTodaySession(GetTodayTrainingSessionQuery query) {
        CognitiveTrainingSession session = sessionRepository
                .findByElderIdAndSessionDate(query.elderId(), LocalDate.now())
                .orElseThrow(() -> new TrainingSessionNotFoundException(query.elderId()));
        return TrainingSessionResult.from(session);
    }

    // F3-01/F3-02: 답안 제출 및 완료 시 난이도 갱신
    @Transactional
    public AnswerResult answerQuestion(AnswerTrainingQuestionCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        QuestionAttempt attempt = session.answer(
                command.questionId(), command.submittedAnswer(), command.responseSeconds());

        if (session.getStatus() == TrainingSessionStatus.COMPLETED) {
            DifficultyProfile profile = profileRepository.findByElderId(session.getElderId())
                    .orElseGet(() -> DifficultyProfile.defaultFor(session.getElderId()));
            profile.applyAttempts(session.getAttempts());
            profileRepository.save(profile);
        }

        sessionRepository.save(session);
        String message = attempt.isCorrect() ? "잘하셨어요!" : "괜찮아요, 다음 문제를 풀어볼까요?";
        return new AnswerResult(TrainingSessionResult.from(session), attempt.isCorrect(), message);
    }

    // F3-03: 손주 찬스 요청
    @Transactional
    public ChanceResult requestGrandchildChance(RequestGrandchildChanceCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        Album album = albumRepository.findById(AlbumId.of(session.getAlbumId()))
                .orElseThrow(() -> new AlbumNotFoundException(session.getAlbumId().toString()));
        int remaining = session.requestGrandchildChance(album.getMemberIds());
        sessionRepository.save(session);
        return new ChanceResult(command.sessionId(), remaining, "가족에게 힌트를 요청했습니다.");
    }

    // F3-03: 가족 힌트 전달
    @Transactional
    public TrainingSessionResult provideHint(ProvideHintCommand command) {
        CognitiveTrainingSession session = loadSessionOrThrow(command.sessionId());
        session.applyHint(command.responderName(), command.hintText());
        sessionRepository.save(session);
        return TrainingSessionResult.from(session);
    }

    private CognitiveTrainingSession loadSessionOrThrow(String sessionId) {
        return sessionRepository.findById(TrainingSessionId.of(sessionId))
                .orElseThrow(() -> new TrainingSessionNotFoundException(sessionId));
    }
}
