package com.memeboo2.haemi.m1.domain.model.album;

public class InviteExpiredException extends RuntimeException {
    public InviteExpiredException() {
        super("초대 링크가 만료되었습니다. 재초대를 요청해주세요.");
    }
}
