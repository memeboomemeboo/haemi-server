package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    @Column(name = "question_difficulty", nullable = false)
    private int difficultyLevel;

    @Column(name = "question_photo_id", columnDefinition = "uuid")
    private UUID photoId;

    private TrainingQuestion(String questionId, QuestionType type, String prompt,
                             int difficultyLevel, UUID photoId) {
        this.questionId = questionId;
        this.type = type;
        this.prompt = prompt;
        this.difficultyLevel = difficultyLevel;
        this.photoId = photoId;
    }

    public static TrainingQuestion of(String questionId, QuestionType type, String prompt,
                                      int difficultyLevel) {
        return new TrainingQuestion(questionId, type, prompt, difficultyLevel, null);
    }

    public static TrainingQuestion withPhoto(String questionId, QuestionType type, String prompt,
                                             int difficultyLevel, UUID photoId) {
        return new TrainingQuestion(questionId, type, prompt, difficultyLevel, photoId);
    }

    public TrainingQuestion copyWithNewId() {
        return new TrainingQuestion(
                "cached-" + UUID.randomUUID(),
                type,
                prompt,
                difficultyLevel,
                photoId
        );
    }
}
