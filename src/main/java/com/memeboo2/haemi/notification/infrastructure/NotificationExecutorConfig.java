package com.memeboo2.haemi.notification.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 알림 발송 전용 스레드 풀 (#80).
 * 발송은 호출자의 트랜잭션 밖에서 돌아야 한다 — FCM 응답을 기다리며 DB 커넥션을 물고 있으면 안 된다.
 *
 * <p>@EnableAsync를 켜지 않고 어댑터에서 이 executor로 직접 넘긴다.
 * 코드베이스에 이미 붙어 있으나 동작하지 않던 @Async(M2 이벤트 리스너)를 이 변경으로 깨우지 않기 위함이다.
 */
@Configuration
public class NotificationExecutorConfig {

    public static final String EXECUTOR_NAME = "notificationExecutor";

    @Bean(name = EXECUTOR_NAME)
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("push-");
        // 큐가 가득 차면 호출 스레드에서 처리한다. 알림을 조용히 버리지 않는다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
