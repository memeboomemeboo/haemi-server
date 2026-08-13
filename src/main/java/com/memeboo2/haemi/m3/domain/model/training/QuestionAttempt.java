package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionAttempt {

    @Column(name = "attempt_question_id", nullable = false)
    private String questionId;

    @Column(name = "vad_duration_ms", nullable = false)
    private int vadDurationMs;

    @Column(name = "responded", nullable = false)
    private boolean responded;

    @Column(name = "response_seconds", nullable = false)
    private int responseSeconds;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    private QuestionAttempt(String questionId, boolean responded, int vadDurationMs,
                            int responseSeconds, LocalDateTime answeredAt) {
        this.questionId = questionId;
        this.responded = responded;
        this.vadDurationMs = Math.max(vadDurationMs, 0);
        this.responseSeconds = responseSeconds;
        this.answeredAt = answeredAt;
    }

    public static QuestionAttempt ofVad(String questionId, boolean voiceDetected, int vadDurationMs) {
        return ofVad(questionId, voiceDetected, vadDurationMs,
                (int) Math.ceil(Math.max(vadDurationMs, 0) / 1_000.0));
    }

    public static QuestionAttempt ofVad(String questionId, boolean voiceDetected, int vadDurationMs,
                                        int responseSeconds) {
        return new QuestionAttempt(questionId, voiceDetected, vadDurationMs,
                Math.max(responseSeconds, 0), LocalDateTime.now());
    }

    /** @deprecated 원문은 버리고 감지 여부와 시간 값만 유지한다. */
    @Deprecated
    public static QuestionAttempt of(String questionId, String ignoredSubmittedAnswer,
                                     boolean responded, int responseSeconds) {
        return ofVad(questionId, responded, 0, responseSeconds);
    }

    public boolean isTimeout() {
        return responseSeconds > 60;
    }
}
