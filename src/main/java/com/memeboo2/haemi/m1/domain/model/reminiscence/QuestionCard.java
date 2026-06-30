package com.memeboo2.haemi.m1.domain.model.reminiscence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCard {

    @Column(name = "question_text", length = 500)
    private String questionText;

    @Column(name = "question_sequence")
    private int sequence;

    private QuestionCard(String questionText, int sequence) {
        this.questionText = questionText;
        this.sequence = sequence;
    }

    public static QuestionCard of(String questionText, int sequence) {
        return new QuestionCard(questionText, sequence);
    }
}
