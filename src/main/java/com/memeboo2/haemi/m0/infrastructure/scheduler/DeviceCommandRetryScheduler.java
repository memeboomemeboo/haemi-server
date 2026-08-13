package com.memeboo2.haemi.m0.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.DeviceCommandDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeviceCommandRetryScheduler {

    private final DeviceCommandDispatchService deviceCommands;

    @Scheduled(cron = "${haemi.device-command.retry-cron:0 */5 * * * *}",
            zone = "${haemi.device-command.time-zone:Asia/Seoul}")
    public void retryDueCommands() {
        deviceCommands.retryDueCommands(LocalDateTime.now());
    }
}
