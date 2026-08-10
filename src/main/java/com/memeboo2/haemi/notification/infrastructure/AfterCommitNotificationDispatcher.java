package com.memeboo2.haemi.notification.infrastructure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

/**
 * 업무 트랜잭션이 성공적으로 커밋된 뒤에만 비동기 알림 작업을 제출한다.
 */
@Component
public class AfterCommitNotificationDispatcher {

    private final Executor notificationExecutor;

    public AfterCommitNotificationDispatcher(
            @Qualifier(NotificationExecutorConfig.EXECUTOR_NAME) Executor notificationExecutor
    ) {
        this.notificationExecutor = notificationExecutor;
    }

    public void execute(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            notificationExecutor.execute(task);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationExecutor.execute(task);
            }
        });
    }
}
