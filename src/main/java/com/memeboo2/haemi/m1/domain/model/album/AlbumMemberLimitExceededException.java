package com.memeboo2.haemi.m1.domain.model.album;

public class AlbumMemberLimitExceededException extends RuntimeException {
    public AlbumMemberLimitExceededException(int maxMembers) {
        super("최대 " + maxMembers + "명까지 초대 가능합니다.");
    }
}
