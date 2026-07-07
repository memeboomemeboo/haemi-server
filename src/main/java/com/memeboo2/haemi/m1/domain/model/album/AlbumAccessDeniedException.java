package com.memeboo2.haemi.m1.domain.model.album;

public class AlbumAccessDeniedException extends RuntimeException {
    public AlbumAccessDeniedException() {
        super("앨범 접근 권한이 없습니다. 그룹 구성원만 접근할 수 있습니다.");
    }
}
