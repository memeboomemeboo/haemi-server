package com.memeboo2.haemi.m1.infrastructure.notification;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.infrastructure.NotificationExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 공용 알림 포트의 FCM 구현 (#80).
 * 호출부는 대부분 @Transactional 서비스 안이라, 발송을 전용 스레드로 넘겨 트랜잭션에서 떼어낸다.
 */
@Component
public class FcmNotificationAdapter implements NotificationPort {

    private final PushDispatchService pushDispatch;
    private final Executor notificationExecutor;

    public FcmNotificationAdapter(PushDispatchService pushDispatch,
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
        // 호출자가 넘긴 컬렉션이 이후에 바뀌어도 안전하도록 복사해 넘긴다.
        Set<String> recipients = Set.copyOf(memberIds);
        notificationExecutor.execute(() -> pushDispatch.dispatchToMembers(recipients, message));
    }
}
