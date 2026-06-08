package com.memeboo2.haemi.m1.domain.event;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContentId;

import java.time.LocalDateTime;

public record ReminiscenceGeneratedEvent(
        ReminiscenceContentId contentId,
        AlbumId albumId,
        LocalDateTime occurredAt
) {}
