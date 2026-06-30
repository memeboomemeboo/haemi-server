package com.memeboo2.haemi.m1.domain.model.album;

public class DuplicatePhotoException extends RuntimeException {
    public DuplicatePhotoException(String hash) {
        super("이미 저장된 사진입니다. hash=" + hash);
    }
}
