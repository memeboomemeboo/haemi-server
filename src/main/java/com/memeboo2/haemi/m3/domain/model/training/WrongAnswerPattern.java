package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WrongAnswerPattern {

    @Column(name = "pattern_key", nullable = false)
    private String patternKey;

    @Column(name = "last_question_id", nullable = false)
    private String lastQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "consecutive_wrong", nullable = false)
    private int consecutiveWrong;

    private WrongAnswerPattern(String patternKey, String lastQuestionId,
                               QuestionType questionType, int consecutiveWrong) {
        this.patternKey = patternKey;
        this.lastQuestionId = lastQuestionId;
        this.questionType = questionType;
        this.consecutiveWrong = consecutiveWrong;
    }

    public static WrongAnswerPattern firstFailure(QuestionPerformance performance) {
        return new WrongAnswerPattern(
                performance.patternKey(),
                performance.questionId(),
                performance.questionType(),
                1
        );
    }

    public void recordFailure(String questionId) {
        this.lastQuestionId = questionId;
        this.consecutiveWrong++;
    }

    public void reset(String questionId) {
        this.lastQuestionId = questionId;
        this.consecutiveWrong = 0;
    }

    public boolean isRepeated() {
        return consecutiveWrong >= 3;
    }
}
