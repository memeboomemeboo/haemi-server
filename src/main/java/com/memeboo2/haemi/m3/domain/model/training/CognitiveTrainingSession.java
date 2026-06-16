package com.memeboo2.haemi.m3.domain.model.training;

import com.memeboo2.haemi.m3.domain.event.HintRequestedEvent;
import com.memeboo2.haemi.m3.domain.event.TrainingSessionCompletedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "cognitive_training_sessions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"elder_id", "session_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CognitiveTrainingSession extends AbstractAggregateRoot<CognitiveTrainingSession> {

    private static final int MIN_QUESTION_COUNT = 3;
    private static final int MAX_QUESTION_COUNT = 5;
    private static final int MAX_CHANCE_PER_SESSION = 2;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_mode", nullable = false)
    private StartMode startMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrainingSessionStatus status;

    @Column(name = "difficulty_level", nullable = false)
    private int difficultyLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "training_questions", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "question_order")
    private List<TrainingQuestion> questions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "training_question_attempts", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "attempt_order")
    private List<QuestionAttempt> attempts = new ArrayList<>();

    @Column(name = "current_question_index", nullable = false)
    private int currentQuestionIndex;

    @Column(name = "chance_used_count", nullable = false)
    private int chanceUsedCount;

    @Column(name = "last_hint_text", length = 500)
    private String lastHintText;

    @Column(name = "last_hint_responder")
    private String lastHintResponder;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static CognitiveTrainingSession start(String elderId, UUID albumId, StartMode startMode,
                                                 int difficultyLevel, List<TrainingQuestion> questions) {
        validateQuestions(questions);
        CognitiveTrainingSession session = new CognitiveTrainingSession();
        session.id = UUID.randomUUID();
        session.elderId = elderId;
        session.albumId = albumId;
        session.sessionDate = LocalDate.now();
        session.startMode = startMode;
        session.status = TrainingSessionStatus.IN_PROGRESS;
        session.difficultyLevel = clampLevel(difficultyLevel);
        session.questions = new ArrayList<>(questions);
        session.currentQuestionIndex = 0;
        session.chanceUsedCount = 0;
        session.startedAt = LocalDateTime.now();
        return session;
    }

    public QuestionAttempt answer(String questionId, String submittedAnswer, int responseSeconds) {
        if (status == TrainingSessionStatus.COMPLETED) {
            throw new TrainingSessionAlreadyCompletedException();
        }
        TrainingQuestion question = currentQuestion()
                .filter(q -> q.getQuestionId().equals(questionId))
                .orElseThrow(() -> new TrainingQuestionNotFoundException(questionId));

        QuestionAttempt attempt = QuestionAttempt.of(
                questionId, submittedAnswer, question.isCorrect(submittedAnswer), responseSeconds);
        attempts.add(attempt);
        currentQuestionIndex++;

        if (currentQuestionIndex >= questions.size()) {
            complete();
        }
        return attempt;
    }

    public int requestGrandchildChance() {
        if (chanceUsedCount >= MAX_CHANCE_PER_SESSION) {
            throw new GrandchildChanceExhaustedException();
        }
        String questionId = currentQuestion()
                .map(TrainingQuestion::getQuestionId)
                .orElseThrow(() -> new TrainingSessionAlreadyCompletedException());
        chanceUsedCount++;
        registerEvent(new HintRequestedEvent(
                id, elderId, albumId, questionId, chanceUsedCount, LocalDateTime.now()));
        return getRemainingChanceCount();
    }

    public void applyHint(String responderName, String hintText) {
        if (hintText == null || hintText.isBlank()) {
            throw new IllegalArgumentException("힌트 내용을 입력해주세요.");
        }
        this.lastHintResponder = responderName;
        this.lastHintText = hintText;
    }

    public Optional<TrainingQuestion> currentQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            return Optional.empty();
        }
        return Optional.of(questions.get(currentQuestionIndex));
    }

    public int getRemainingChanceCount() {
        return Math.max(0, MAX_CHANCE_PER_SESSION - chanceUsedCount);
    }

    public double getAccuracyRate() {
        if (attempts.isEmpty()) return 0.0;
        long correct = attempts.stream().filter(QuestionAttempt::isCorrect).count();
        return (double) correct / attempts.size();
    }

    public double getAverageResponseSeconds() {
        return attempts.stream()
                .mapToInt(QuestionAttempt::getResponseSeconds)
                .average()
                .orElse(0.0);
    }

    public int getCorrectCount() {
        return (int) attempts.stream().filter(QuestionAttempt::isCorrect).count();
    }

    public int getWrongCount() {
        return attempts.size() - getCorrectCount();
    }

    public TrainingSessionId getSessionId() {
        return TrainingSessionId.of(id);
    }

    public List<TrainingQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public List<QuestionAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    private void complete() {
        this.status = TrainingSessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        registerEvent(new TrainingSessionCompletedEvent(
                id, elderId, albumId, sessionDate, getAccuracyRate(),
                getAverageResponseSeconds(), attempts.size(), completedAt));
    }

    private static void validateQuestions(List<TrainingQuestion> questions) {
        if (questions == null || questions.size() < MIN_QUESTION_COUNT || questions.size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("인지 훈련 문제는 3~5개여야 합니다.");
        }
        for (int i = 1; i < questions.size(); i++) {
            if (questions.get(i - 1).getType() == questions.get(i).getType()) {
                throw new IllegalArgumentException("같은 유형의 문제가 연속 배치될 수 없습니다.");
            }
        }
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(5, level));
    }
}
