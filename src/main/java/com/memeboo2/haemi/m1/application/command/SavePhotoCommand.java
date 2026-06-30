package com.memeboo2.haemi.m1.application.command;

import java.io.InputStream;
import java.time.LocalDateTime;

public record SavePhotoCommand(
        String albumId,
        String uploadedBy,
        InputStream inputStream,
        String originalFilename,
        String contentType,
        long fileSize,
        String sha256Hash,
        LocalDateTime shotAt,
        Double latitude,
        Double longitude
) {}
