package com.memeboo2.haemi.m1.domain.model.album;

public class MemberNotInvitedException extends RuntimeException {
    public MemberNotInvitedException(String memberId) {
        super("초대 내역을 찾을 수 없습니다. memberId=" + memberId);
    }
}
