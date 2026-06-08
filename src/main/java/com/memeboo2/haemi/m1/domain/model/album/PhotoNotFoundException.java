package com.memeboo2.haemi.m1.domain.model.album;

public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(PhotoId photoId) {
        super("사진을 찾을 수 없습니다. id=" + photoId);
    }
}
