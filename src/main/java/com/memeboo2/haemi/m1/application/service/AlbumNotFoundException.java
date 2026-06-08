package com.memeboo2.haemi.m1.application.service;

public class AlbumNotFoundException extends RuntimeException {
    public AlbumNotFoundException(String albumId) {
        super("앨범을 찾을 수 없습니다. id=" + albumId);
    }
}
