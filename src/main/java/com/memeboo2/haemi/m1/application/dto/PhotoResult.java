package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.album.AnalysisStatus;
import com.memeboo2.haemi.m1.domain.model.album.Photo;

import java.time.LocalDateTime;
import java.util.List;

public record PhotoResult(
        String photoId,
        String storageKey,
        String originalFilename,
        String contentType,
        long fileSize,
        LocalDateTime shotAt,
        Double latitude,
        Double longitude,
        String timePeriod,
        String locationText,
        String memo,
        List<PersonTagResult> personTags,
        AnalysisStatus analysisStatus,
        String uploadedBy,
        LocalDateTime uploadedAt
) {
    public record PersonTagResult(String memberId, String memberName) {}

    public static PhotoResult from(Photo photo) {
        List<PersonTagResult> tags = photo.getPersonTags().stream()
                .map(t -> new PersonTagResult(t.getMemberId(), t.getMemberName()))
                .toList();
        return new PhotoResult(
                photo.getId().toString(),
                photo.getFile().getStorageKey(),
                photo.getFile().getOriginalFilename(),
                photo.getFile().getContentType(),
                photo.getFile().getFileSize(),
                photo.getMetadata().getShotAt(),
                photo.getMetadata().getLatitude(),
                photo.getMetadata().getLongitude(),
                photo.getMetadata().getTimePeriod(),
                photo.getMetadata().getLocationText(),
                photo.getMetadata().getMemo(),
                tags,
                photo.getAnalysisStatus(),
                photo.getUploadedBy(),
                photo.getUploadedAt()
        );
    }
}
