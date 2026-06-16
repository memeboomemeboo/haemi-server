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

    @Column(name = "submitted_answer", length = 200)
    private String submittedAnswer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "response_seconds", nullable = false)
    private int responseSeconds;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    private QuestionAttempt(String questionId, String submittedAnswer, boolean correct,
                            int responseSeconds, LocalDateTime answeredAt) {
        this.questionId = questionId;
        this.submittedAnswer = submittedAnswer;
        this.correct = correct;
        this.responseSeconds = responseSeconds;
        this.answeredAt = answeredAt;
    }

    public static QuestionAttempt of(String questionId, String submittedAnswer,
                                     boolean correct, int responseSeconds) {
        return new QuestionAttempt(questionId, submittedAnswer, correct,
                Math.max(responseSeconds, 0), LocalDateTime.now());
    }

    public boolean isTimeout() {
        return responseSeconds > 60;
    }
}
