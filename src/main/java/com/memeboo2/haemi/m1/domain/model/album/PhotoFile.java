package com.memeboo2.haemi.m1.domain.model.album;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoFile {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/heic", "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    private PhotoFile(String storageKey, String originalFilename, String contentType, long fileSize) {
        validate(contentType, fileSize);
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public static PhotoFile of(String storageKey, String originalFilename, String contentType, long fileSize) {
        return new PhotoFile(storageKey, originalFilename, contentType, fileSize);
    }

    private static void validate(String contentType, long fileSize) {
        if (!SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedPhotoFormatException(contentType);
        }
        if (fileSize > MAX_SIZE_BYTES) {
            throw new PhotoFileSizeExceededException(fileSize);
        }
    }
}
