package com.memeboo2.haemi.m2.infrastructure.notification;

import com.memeboo2.haemi.m2.domain.port.PushNotificationPort;
import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.infrastructure.AfterCommitNotificationDispatcher;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * M2 푸시 포트의 FCM 구현 (#80).
 */
@Component
public class FcmPushNotificationAdapter implements PushNotificationPort {

    private final PushDispatchService pushDispatch;
    private final AfterCommitNotificationDispatcher notificationDispatcher;

    public FcmPushNotificationAdapter(PushDispatchService pushDispatch,
                                      AfterCommitNotificationDispatcher notificationDispatcher) {
        this.pushDispatch = pushDispatch;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Override
    public void sendToMember(String memberId, String title, String body) {
        PushMessage message = PushMessage.of(title, body);
        notificationDispatcher.execute(() -> pushDispatch.dispatchToMember(memberId, message));
    }

    @Override
    public void sendToGroup(Set<String> memberIds, String title, String body) {
        PushMessage message = PushMessage.of(title, body);
        Set<String> recipients = Set.copyOf(memberIds);
        notificationDispatcher.execute(() -> pushDispatch.dispatchToMembers(recipients, message));
    }
}
