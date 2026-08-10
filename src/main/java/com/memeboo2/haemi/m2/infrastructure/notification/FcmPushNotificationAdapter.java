package com.memeboo2.haemi.m2.infrastructure.notification;

import com.memeboo2.haemi.m2.domain.port.PushNotificationPort;
import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.infrastructure.NotificationExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * M2 푸시 포트의 FCM 구현 (#80).
 */
@Component
public class FcmPushNotificationAdapter implements PushNotificationPort {

    private final PushDispatchService pushDispatch;
    private final Executor notificationExecutor;

    public FcmPushNotificationAdapter(PushDispatchService pushDispatch,
                                      @Qualifier(NotificationExecutorConfig.EXECUTOR_NAME) Executor notificationExecutor) {
        this.pushDispatch = pushDispatch;
        this.notificationExecutor = notificationExecutor;
    }

    @Override
    public void sendToMember(String memberId, String title, String body) {
        PushMessage message = PushMessage.of(title, body);
        notificationExecutor.execute(() -> pushDispatch.dispatchToMember(memberId, message));
    }

    @Override
    public void sendToGroup(Set<String> memberIds, String title, String body) {
        PushMessage message = PushMessage.of(title, body);
        Set<String> recipients = Set.copyOf(memberIds);
        notificationExecutor.execute(() -> pushDispatch.dispatchToMembers(recipients, message));
    }
}
