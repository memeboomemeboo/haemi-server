package com.memeboo2.haemi.m1.domain.model.reminiscence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlideCard {

    @Column(name = "slide_photo_id")
    private UUID photoId;

    @Column(name = "slide_sequence")
    private int sequence;

    @Column(name = "slide_caption", length = 300)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", length = 20)
    private CardType cardType;

    @Column(name = "safety_passed", nullable = false)
    private boolean safetyPassed;

    private SlideCard(UUID photoId, int sequence, String caption, CardType cardType) {
        this.photoId = photoId;
        this.sequence = sequence;
        this.caption = caption;
        this.cardType = cardType;
        this.safetyPassed = true;
    }

    public static SlideCard of(UUID photoId, int sequence, String caption) {
        return new SlideCard(photoId, sequence, caption, CardType.STORY_CARD);
    }

    public static SlideCard of(GeneratedCard card, int sequence) {
        return new SlideCard(card.photoId(), sequence, card.promptText(), card.cardType());
    }
}
