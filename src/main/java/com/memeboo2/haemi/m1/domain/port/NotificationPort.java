package com.memeboo2.haemi.m1.domain.port;

import java.util.Set;

public interface NotificationPort {

    void sendToMember(String memberId, String title, String body);

    void sendToGroup(Set<String> memberIds, String title, String body);
}
