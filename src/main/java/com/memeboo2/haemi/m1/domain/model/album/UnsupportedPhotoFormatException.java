package com.memeboo2.haemi.m1.domain.model.album;

public class UnsupportedPhotoFormatException extends RuntimeException {
    public UnsupportedPhotoFormatException(String contentType) {
        super("지원하지 않는 형식입니다(JPG/PNG/HEIC/WebP). contentType=" + contentType);
    }
}
