package com.memeboo2.haemi.m2.application.command;

import java.io.InputStream;
import java.util.List;

public record CreateMemoryPostCommand(
        String albumId,
        String memberId,
        String memberName,
        String relation,
        String textContent,
        List<PhotoAttachment> photos,
        VoiceMemo voiceMemo,
        boolean publishImmediately  // false면 임시 저장
) {
    public record PhotoAttachment(
            InputStream inputStream,
            String originalFilename,
            String contentType,
            long fileSize
    ) {}

    public record VoiceMemo(
            InputStream inputStream,
            String contentType,
            long durationSeconds
    ) {}
}
