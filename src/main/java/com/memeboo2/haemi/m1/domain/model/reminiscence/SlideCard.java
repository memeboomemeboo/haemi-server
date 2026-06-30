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
public class SlideCard {

    @Column(name = "slide_photo_id")
    private UUID photoId;

    @Column(name = "slide_sequence")
    private int sequence;

    @Column(name = "slide_caption", length = 300)
    private String caption;

    private SlideCard(UUID photoId, int sequence, String caption) {
        this.photoId = photoId;
        this.sequence = sequence;
        this.caption = caption;
    }

    public static SlideCard of(UUID photoId, int sequence, String caption) {
        return new SlideCard(photoId, sequence, caption);
    }
}
