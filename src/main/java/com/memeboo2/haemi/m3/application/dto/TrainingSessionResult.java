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
        int difficultyLevel,
        int currentQuestionIndex,
        int remainingChanceCount,
        QuestionResult currentQuestion,
        List<QuestionResult> questions,
        List<AttemptResult> attempts,
        double accuracyRate,
        double averageResponseSeconds,
        SpeechGuideResult speechGuide,
        String lastHintResponder,
        String lastHintText,
        GrandchildChanceStatus lastChanceStatus,
        String lastChanceQuestionId,
        LocalDateTime lastChanceRequestedAt,
        boolean chanceUnusedCompletionBadgeAwarded,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public record QuestionResult(
            String questionId,
            QuestionType type,
            String prompt,
            int difficultyLevel,
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

    public record AttemptResult(
            String questionId,
            String submittedAnswer,
            boolean correct,
            int responseSeconds,
            LocalDateTime answeredAt
    ) {}

    public static TrainingSessionResult from(CognitiveTrainingSession session, TrainingSpeech speech) {
        List<QuestionResult> questions = session.getQuestions().stream()
                .map(q -> new QuestionResult(q.getQuestionId(), q.getType(),
                        q.getPrompt(), q.getDifficultyLevel(),
                        q.getPhotoId() == null ? null : q.getPhotoId().toString()))
                .toList();
        List<AttemptResult> attempts = session.getAttempts().stream()
                .map(a -> new AttemptResult(a.getQuestionId(), a.getSubmittedAnswer(),
                        a.isCorrect(), a.getResponseSeconds(), a.getAnsweredAt()))
                .toList();
        QuestionResult current = session.currentQuestion()
                .map(q -> new QuestionResult(q.getQuestionId(), q.getType(),
                        q.getPrompt(), q.getDifficultyLevel(),
                        q.getPhotoId() == null ? null : q.getPhotoId().toString()))
                .orElse(null);
        return new TrainingSessionResult(
                session.getId().toString(),
                session.getElderId(),
                session.getAlbumId().toString(),
                session.getSessionDate(),
                session.getStartMode(),
                session.getStatus(),
                session.getDifficultyLevel(),
                session.getCurrentQuestionIndex(),
                session.getRemainingChanceCount(),
                current,
                questions,
                attempts,
                session.getAccuracyRate(),
                session.getAverageResponseSeconds(),
                SpeechGuideResult.from(speech),
                session.getLastHintResponder(),
                session.getLastHintText(),
                session.getLastChanceStatus(),
                session.getLastChanceQuestionId(),
                session.getLastChanceRequestedAt(),
                session.isChanceUnusedCompletionBadgeAwarded(),
                session.getStartedAt(),
                session.getCompletedAt()
        );
    }
}
