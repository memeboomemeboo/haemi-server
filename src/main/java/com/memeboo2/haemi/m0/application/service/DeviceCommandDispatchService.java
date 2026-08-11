package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.DeviceCommand;
import com.memeboo2.haemi.m0.domain.port.DeviceLockPort;
import com.memeboo2.haemi.m0.domain.repository.DeviceCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** F0-05 기기 명령을 아웃박스로 저장하고 전달 완료까지 재시도한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandDispatchService {

    private final DeviceCommandRepository commands;
    private final DeviceLockPort deviceLockPort;

    @Value("${haemi.device-command.max-attempts:10}")
    private int maxAttempts;

    @Transactional
    public void enqueueBereavementLock(UUID elderId) {
        DeviceCommand command = commands.save(DeviceCommand.lockAndOpenMemorial(elderId, LocalDateTime.now()));
        dispatch(command, LocalDateTime.now());
    }

    @Transactional
    public int retryDueCommands(LocalDateTime now) {
        int attempted = 0;
        for (DeviceCommand command : commands.findPendingBefore(now)) {
            if (!command.isRetryableAt(now, maxAttempts)) {
                continue;
            }
            dispatch(command, now);
            attempted++;
        }
        return attempted;
    }

    private void dispatch(DeviceCommand command, LocalDateTime now) {
        try {
            deviceLockPort.lock(command.getElderId());
            command.delivered(now);
        } catch (Exception exception) {
            command.failed(now, exception.getMessage());
            log.warn("기기 명령 전달 실패, 재시도 예정: commandId={}, elderId={}, attempts={}",
                    command.getId(), command.getElderId(), command.getAttempts(), exception);
        }
        commands.save(command);
    }
}
