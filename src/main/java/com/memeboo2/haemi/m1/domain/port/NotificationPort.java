package com.memeboo2.haemi.m1.domain.port;

import java.util.Set;

public interface NotificationPort {

    void sendToMember(String memberId, String title, String body);

    /** 어르신 프로필에 연결된 본인 휴대전화로 발송한다. */
    void sendToElder(String elderId, String title, String body);

    void sendToGroup(Set<String> memberIds, String title, String body);
}
