package com.memeboo2.haemi.m3.domain.model.training;

import com.memeboo2.haemi.common.exception.DomainValidationException;

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
    private static final int REALTIME_HINT_RESPONSE_LIMIT_SECONDS = 60;

    // F3-01 회상 세션 타이밍 (클라이언트 재생/힌트 노출 기준)
    public static final int HINT_DELAY_SECONDS = 4;
    public static final int AUTO_PLAY_DELAY_SECONDS = 7;
    public static final int NO_RESPONSE_ALLOWANCE_SECONDS = 60;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "last_chance_status", nullable = false)
    private GrandchildChanceStatus lastChanceStatus;

    @Column(name = "last_chance_question_id")
    private String lastChanceQuestionId;

    @Column(name = "last_chance_requested_at")
    private LocalDateTime lastChanceRequestedAt;

    /** L2 실시간 요청을 받은 가족 한 명. 응답 위조를 막기 위해 서버에서만 검증한다. */
    @Column(name = "last_chance_recipient_member_id")
    private String lastChanceRecipientMemberId;

    @Column(name = "last_hint_text", length = 500)
    private String lastHintText;

    @Column(name = "last_hint_responder")
    private String lastHintResponder;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "chance_unused_completion_badge_awarded", nullable = false)
    private boolean chanceUnusedCompletionBadgeAwarded;

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
        session.lastChanceStatus = GrandchildChanceStatus.NONE;
        session.chanceUnusedCompletionBadgeAwarded = false;
        session.startedAt = LocalDateTime.now();
        return session;
    }

    /** 단말 VAD 결과만 기록한다. 음성 원문/전사문은 이 도메인에 들어오지 않는다. */
    public QuestionAttempt answer(String questionId, boolean voiceDetected, int vadDurationMs) {
        if (status == TrainingSessionStatus.COMPLETED) {
            throw new TrainingSessionAlreadyCompletedException();
        }
        expireGrandchildChanceIfNeeded(LocalDateTime.now());
        TrainingQuestion question = currentQuestion()
                .filter(q -> q.getQuestionId().equals(questionId))
                .orElseThrow(() -> new TrainingQuestionNotFoundException(questionId));

        QuestionAttempt attempt = QuestionAttempt.ofVad(questionId, voiceDetected, vadDurationMs);
        attempts.add(attempt);
        currentQuestionIndex++;

        if (currentQuestionIndex >= questions.size()) {
            complete();
        }
        return attempt;
    }

    /** @deprecated 원문은 버리고 VAD 신호로만 변환하는 내부 호환 경로다. */
    @Deprecated
    public QuestionAttempt answer(String questionId, String ignoredSubmittedAnswer, int responseSeconds) {
        return answer(questionId, ignoredSubmittedAnswer != null && !ignoredSubmittedAnswer.isBlank(),
                Math.max(responseSeconds, 0) * 1_000);
    }

    /** 사별·입원 전이 뒤에는 진행 중 세션을 즉시 중단한다. */
    public void abandonForElderStatus() {
        if (status == TrainingSessionStatus.IN_PROGRESS) {
            status = TrainingSessionStatus.ABANDONED;
            completedAt = LocalDateTime.now();
        }
    }

    // F3-01: 60초 무응답 허용 — 발화 없이 다음 사진으로 진행 (손주 찬스 게이팅과 무관)
    public QuestionAttempt recordNoResponse(String questionId) {
        if (status == TrainingSessionStatus.COMPLETED) {
            throw new TrainingSessionAlreadyCompletedException();
        }
        expireGrandchildChanceIfNeeded(LocalDateTime.now());
        TrainingQuestion question = currentQuestion()
                .filter(q -> q.getQuestionId().equals(questionId))
                .orElseThrow(() -> new TrainingQuestionNotFoundException(questionId));
        QuestionAttempt attempt = QuestionAttempt.ofVad(
                question.getQuestionId(), false, 0, NO_RESPONSE_ALLOWANCE_SECONDS);
        attempts.add(attempt);
        currentQuestionIndex++;
        if (currentQuestionIndex >= questions.size()) {
            complete();
        }
        return attempt;
    }

    public QuestionAttempt passCurrentQuestion() {
        expireGrandchildChanceIfNeeded(LocalDateTime.now());
        if (lastChanceStatus != GrandchildChanceStatus.EXPIRED) {
            throw new TrainingQuestionPassUnavailableException();
        }
        TrainingQuestion question = currentQuestion()
                .orElseThrow(TrainingSessionAlreadyCompletedException::new);
        QuestionAttempt attempt = QuestionAttempt.ofVad(question.getQuestionId(), false, 0, 61);
        // 손주 찬스 만료 후 넘긴 문제는 무응답으로 기록
        attempts.add(attempt);
        currentQuestionIndex++;
        if (currentQuestionIndex >= questions.size()) {
            complete();
        }
        return attempt;
    }

    public void requestGrandchildChance(Set<String> recipientMemberIds) {
        requestGrandchildChance(recipientMemberIds, LocalDateTime.now());
    }

    void requestGrandchildChance(Set<String> recipientMemberIds, LocalDateTime requestedAt) {
        if (recipientMemberIds == null || recipientMemberIds.isEmpty()) {
            throw new GrandchildChanceUnavailableException();
        }
        expireGrandchildChanceIfNeeded(requestedAt);
        String questionId = currentQuestion()
                .map(TrainingQuestion::getQuestionId)
                .orElseThrow(() -> new TrainingSessionAlreadyCompletedException());
        lastChanceStatus = GrandchildChanceStatus.PENDING;
        lastChanceQuestionId = questionId;
        lastChanceRequestedAt = requestedAt;
        lastChanceRecipientMemberId = recipientMemberIds.size() == 1
                ? recipientMemberIds.iterator().next()
                : null;
        registerEvent(new HintRequestedEvent(
                id, elderId, albumId, questionId, 0, recipientMemberIds, requestedAt));
    }

    // F3-03: 사전 적립형 손주 한마디 — 대기 없이 즉시 제공 (세션당 사용 제한 없음)
    public void serveAccruedHint(String hintText, String responderName) {
        if (status == TrainingSessionStatus.COMPLETED) {
            throw new TrainingSessionAlreadyCompletedException();
        }
        if (hintText == null || hintText.isBlank()) {
            throw new DomainValidationException("힌트 내용이 없습니다.");
        }
        currentQuestion().orElseThrow(TrainingSessionAlreadyCompletedException::new);
        lastChanceStatus = GrandchildChanceStatus.ANSWERED;
        this.lastHintText = hintText;
        this.lastHintResponder = responderName;
    }

    public void applyHint(String responderName, String hintText) {
        applyHint(lastChanceRecipientMemberId, responderName, hintText, LocalDateTime.now());
    }

    void applyHint(String responderName, String hintText, LocalDateTime respondedAt) {
        applyHint(lastChanceRecipientMemberId, responderName, hintText, respondedAt);
    }

    public void applyHint(String responderMemberId, String responderName, String hintText) {
        applyHint(responderMemberId, responderName, hintText, LocalDateTime.now());
    }

    void applyHint(String responderMemberId, String responderName, String hintText, LocalDateTime respondedAt) {
        if (hintText == null || hintText.isBlank()) {
            throw new DomainValidationException("힌트 내용을 입력해주세요.");
        }
        expireGrandchildChanceIfNeeded(respondedAt);
        if (lastChanceStatus == GrandchildChanceStatus.EXPIRED) {
            throw new GrandchildChanceExpiredException();
        }
        if (lastChanceStatus != GrandchildChanceStatus.PENDING) {
            throw new DomainValidationException("진행 중인 손주 찬스 요청이 없습니다.");
        }
        if (lastChanceRecipientMemberId != null && !lastChanceRecipientMemberId.equals(responderMemberId)) {
            throw new GrandchildChanceResponderNotRecipientException();
        }
        lastChanceStatus = GrandchildChanceStatus.ANSWERED;
        this.lastHintResponder = responderName;
        this.lastHintText = hintText;
    }

    public Optional<TrainingQuestion> currentQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            return Optional.empty();
        }
        return Optional.of(questions.get(currentQuestionIndex));
    }

    public boolean isGrandchildChancePending() {
        return lastChanceStatus == GrandchildChanceStatus.PENDING;
    }

    public boolean isLastGrandchildChanceExpired() {
        return lastChanceStatus == GrandchildChanceStatus.EXPIRED;
    }

    public boolean refreshGrandchildChanceStatus(LocalDateTime now) {
        GrandchildChanceStatus previousStatus = lastChanceStatus;
        expireGrandchildChanceIfNeeded(now);
        return previousStatus != lastChanceStatus;
    }

    public double getResponseRate() {
        if (attempts.isEmpty()) return 0.0;
        long responded = attempts.stream().filter(QuestionAttempt::isResponded).count();
        return (double) responded / attempts.size();
    }

    public double getAverageResponseSeconds() {
        return attempts.stream()
                .mapToInt(QuestionAttempt::getResponseSeconds)
                .average()
                .orElse(0.0);
    }

    public int getRespondedCount() {
        return (int) attempts.stream().filter(QuestionAttempt::isResponded).count();
    }

    public int getNoResponseCount() {
        return attempts.size() - getRespondedCount();
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

    public List<QuestionPerformance> getQuestionPerformances() {
        Map<String, TrainingQuestion> questionsById = questions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        TrainingQuestion::getQuestionId,
                        question -> question
                ));
        return attempts.stream()
                .map(attempt -> {
                    TrainingQuestion question = questionsById.get(attempt.getQuestionId());
                    if (question == null) {
                        throw new TrainingQuestionNotFoundException(attempt.getQuestionId());
                    }
                    return new QuestionPerformance(
                            attempt.getQuestionId(),
                            question.getType(),
                            attempt.isResponded(),
                            attempt.isTimeout()
                    );
                })
                .toList();
    }

    private void complete() {
        this.status = TrainingSessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        registerEvent(new TrainingSessionCompletedEvent(
                id, elderId, albumId, sessionDate, getResponseRate(),
                getAverageResponseSeconds(), attempts.size(), completedAt));
    }

    private void expireGrandchildChanceIfNeeded(LocalDateTime now) {
        if (lastChanceStatus != GrandchildChanceStatus.PENDING || lastChanceRequestedAt == null) {
            return;
        }
        if (!lastChanceRequestedAt.plusSeconds(REALTIME_HINT_RESPONSE_LIMIT_SECONDS).isAfter(now)) {
            lastChanceStatus = GrandchildChanceStatus.EXPIRED;
        }
    }

    private static void validateQuestions(List<TrainingQuestion> questions) {
        if (questions == null || questions.size() < MIN_QUESTION_COUNT || questions.size() > MAX_QUESTION_COUNT) {
            throw new DomainValidationException("인지 훈련 문제는 3~5개여야 합니다.");
        }
        for (int i = 1; i < questions.size(); i++) {
            if (questions.get(i - 1).getType() == questions.get(i).getType()) {
                throw new DomainValidationException("같은 유형의 문제가 연속 배치될 수 없습니다.");
            }
        }
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(5, level));
    }

}
