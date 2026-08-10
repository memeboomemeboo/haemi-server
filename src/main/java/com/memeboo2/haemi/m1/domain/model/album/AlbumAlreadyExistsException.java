package com.memeboo2.haemi.m1.domain.model.album;

public class AlbumAlreadyExistsException extends RuntimeException {

    public AlbumAlreadyExistsException() {
        super("이 가족 그룹의 앨범은 이미 생성되어 있어요.");
    }
}
