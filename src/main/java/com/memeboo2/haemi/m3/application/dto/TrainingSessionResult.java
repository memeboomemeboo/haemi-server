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
        String lastHintResponder,
        String lastHintText,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public record QuestionResult(
            String questionId,
            QuestionType type,
            String prompt,
            int difficultyLevel
    ) {}

    public record AttemptResult(
            String questionId,
            String submittedAnswer,
            boolean correct,
            int responseSeconds,
            LocalDateTime answeredAt
    ) {}

    public static TrainingSessionResult from(CognitiveTrainingSession session) {
        List<QuestionResult> questions = session.getQuestions().stream()
                .map(q -> new QuestionResult(q.getQuestionId(), q.getType(),
                        q.getPrompt(), q.getDifficultyLevel()))
                .toList();
        List<AttemptResult> attempts = session.getAttempts().stream()
                .map(a -> new AttemptResult(a.getQuestionId(), a.getSubmittedAnswer(),
                        a.isCorrect(), a.getResponseSeconds(), a.getAnsweredAt()))
                .toList();
        QuestionResult current = session.currentQuestion()
                .map(q -> new QuestionResult(q.getQuestionId(), q.getType(),
                        q.getPrompt(), q.getDifficultyLevel()))
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
                session.getLastHintResponder(),
                session.getLastHintText(),
                session.getStartedAt(),
                session.getCompletedAt()
        );
    }
}
