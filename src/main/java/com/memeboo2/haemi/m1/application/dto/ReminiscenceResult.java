package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.reminiscence.CardType;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReactionType;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReminiscenceResult(
        String contentId,
        String albumId,
        LocalDate generatedDate,
        LocalDateTime generatedAt,
        List<CardResult> cards,
        ReactionType elderReaction
) {
    public record CardResult(UUID photoId, int sequence, CardType cardType, String promptText) {}

    public static ReminiscenceResult from(ReminiscenceContent content) {
        List<CardResult> cards = content.getSlideCards().stream()
                .map(s -> new CardResult(s.getPhotoId(), s.getSequence(), s.getCardType(), s.getCaption()))
                .toList();
        return new ReminiscenceResult(
                content.getId().toString(),
                content.getAlbumId().toString(),
                content.getGeneratedDate(),
                content.getGeneratedAt(),
                cards,
                content.getElderReaction()
        );
    }
}
