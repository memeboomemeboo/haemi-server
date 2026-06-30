package com.memeboo2.haemi.m1.domain.event;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.PhotoId;

import java.time.LocalDateTime;

public record PhotoAnalysisCompletedEvent(
        AlbumId albumId,
        PhotoId photoId,
        LocalDateTime occurredAt
) {}
