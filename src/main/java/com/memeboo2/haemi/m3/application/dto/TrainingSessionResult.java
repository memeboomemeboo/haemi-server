package com.memeboo2.haemi.m3.application.dto;

import com.memeboo2.haemi.m3.domain.model.training.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingSessionResult(
        String sessionId,
        String elderId,
        String albumId,
        LocalDate sessionDate,
        StartMode startMode,
        TrainingSessionStatus status,
        int currentQuestionIndex,
        int remainingChanceCount,
        QuestionResult currentQuestion,
        List<QuestionResult> questions,
        List<AttemptResult> attempts,
        double responseRate,
        double averageResponseSeconds,
        SpeechGuideResult speechGuide,
        RecallTiming recallTiming,
        String lastHintResponder,
        String lastHintText,
        GrandchildChanceStatus lastChanceStatus,
        String lastChanceQuestionId,
        LocalDateTime lastChanceRequestedAt,
        String grandchildChanceGuideMessage,
        boolean questionPassAvailable,
        boolean chanceUnusedCompletionBadgeAwarded,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    // F3-02: 난이도 레벨은 어르신·가족에게 노출하지 않는다 (EX-F302-07)
    public record QuestionResult(
            String questionId,
            QuestionType type,
            String prompt,
            String photoId
    ) {}

    public record SpeechGuideResult(
            String text,
            String ssml,
            String locale,
            double speechRate,
            boolean autoPlay
    ) {
        public static SpeechGuideResult from(TrainingSpeech speech) {
            return new SpeechGuideResult(
                    speech.text(),
                    speech.ssml(),
                    speech.locale(),
                    speech.speechRate(),
                    true
            );
        }
    }

    public record RecallTiming(
            int hintDelaySeconds,
            int autoPlayDelaySeconds,
            int noResponseAllowanceSeconds
    ) {
        public static RecallTiming defaults() {
            return new RecallTiming(
                    CognitiveTrainingSession.HINT_DELAY_SECONDS,
                    CognitiveTrainingSession.AUTO_PLAY_DELAY_SECONDS,
                    CognitiveTrainingSession.NO_RESPONSE_ALLOWANCE_SECONDS
            );
        }
    }

    public record AttemptResult(
            String questionId,
            String submittedAnswer,
            boolean responded,
            int responseSeconds,
            LocalDateTime answeredAt
    ) {}

    public static TrainingSessionResult from(CognitiveTrainingSession session, TrainingSpeech speech) {
        List<QuestionResult> questions = session.getQuestions().stream()
                .map(q -> new QuestionResult(q.getQuestionId(), q.getType(),
                        q.getPrompt(),
                        q.getPhotoId() == null ? null : q.getPhotoId().toString()))
                .toList();
        List<AttemptResult> attempts = session.getAttempts().stream()
                .map(a -> new AttemptResult(a.getQuestionId(), a.getSubmittedAnswer(),
                        a.isResponded(), a.getResponseSeconds(), a.getAnsweredAt()))
                .toList();
        QuestionResult current = session.currentQuestion()
                .map(q -> new QuestionResult(q.getQuestionId(), q.getType(),
                        q.getPrompt(),
                        q.getPhotoId() == null ? null : q.getPhotoId().toString()))
                .orElse(null);
        return new TrainingSessionResult(
                session.getId().toString(),
                session.getElderId(),
                session.getAlbumId().toString(),
                session.getSessionDate(),
                session.getStartMode(),
                session.getStatus(),
                session.getCurrentQuestionIndex(),
                session.getRemainingChanceCount(),
                current,
                questions,
                attempts,
                session.getResponseRate(),
                session.getAverageResponseSeconds(),
                SpeechGuideResult.from(speech),
                RecallTiming.defaults(),
                session.getLastHintResponder(),
                session.getLastHintText(),
                session.getLastChanceStatus(),
                session.getLastChanceQuestionId(),
                session.getLastChanceRequestedAt(),
                grandchildChanceGuideMessage(session),
                session.isLastGrandchildChanceExpired(),
                session.isChanceUnusedCompletionBadgeAwarded(),
                session.getStartedAt(),
                session.getCompletedAt()
        );
    }

    private static String grandchildChanceGuideMessage(CognitiveTrainingSession session) {
        if (session.isLastGrandchildChanceExpired()) {
            return "가족이 바쁜가봐요, 조금 더 생각해볼까요?";
        }
        return null;
    }
}
