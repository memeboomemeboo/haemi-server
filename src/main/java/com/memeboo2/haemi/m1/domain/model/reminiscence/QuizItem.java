package com.memeboo2.haemi.m1.domain.model.reminiscence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizItem {

    @Column(name = "quiz_photo_id")
    private UUID photoId;

    @Column(name = "quiz_text", length = 500)
    private String quizText;

    @Column(name = "quiz_answer", length = 200)
    private String answer;

    @Column(name = "quiz_sequence")
    private int sequence;

    private QuizItem(UUID photoId, String quizText, String answer, int sequence) {
        this.photoId = photoId;
        this.quizText = quizText;
        this.answer = answer;
        this.sequence = sequence;
    }

    public static QuizItem of(UUID photoId, String quizText, String answer, int sequence) {
        return new QuizItem(photoId, quizText, answer, sequence);
    }
}
