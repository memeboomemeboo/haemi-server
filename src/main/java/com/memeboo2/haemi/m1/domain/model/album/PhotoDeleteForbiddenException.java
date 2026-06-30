package com.memeboo2.haemi.m1.domain.model.album;

public class PhotoDeleteForbiddenException extends RuntimeException {
    public PhotoDeleteForbiddenException() {
        super("삭제 권한이 없습니다. 앨범 소유자에게 문의하세요.");
    }
}
