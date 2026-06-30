package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingQuestion {

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType type;

    @Column(name = "question_prompt", nullable = false, length = 500)
    private String prompt;

    @Column(name = "correct_answer", nullable = false, length = 200)
    private String correctAnswer;

    @Column(name = "question_difficulty", nullable = false)
    private int difficultyLevel;

    private TrainingQuestion(String questionId, QuestionType type, String prompt,
                             String correctAnswer, int difficultyLevel) {
        this.questionId = questionId;
        this.type = type;
        this.prompt = prompt;
        this.correctAnswer = correctAnswer;
        this.difficultyLevel = difficultyLevel;
    }

    public static TrainingQuestion of(String questionId, QuestionType type, String prompt,
                                      String correctAnswer, int difficultyLevel) {
        return new TrainingQuestion(questionId, type, prompt, correctAnswer, difficultyLevel);
    }

    public boolean isCorrect(String submittedAnswer) {
        return submittedAnswer != null
                && correctAnswer.trim().equalsIgnoreCase(submittedAnswer.trim());
    }
}
