package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 기기 명령 아웃박스. 사별 상태를 먼저 확정한 뒤 명령을 재시도할 수 있도록 발송 결과를 보관한다.
 */
@Entity
@Table(name = "device_commands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceCommand {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DeviceCommandAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceCommandStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static DeviceCommand lockAndOpenMemorial(UUID elderId, LocalDateTime now) {
        DeviceCommand command = new DeviceCommand();
        command.id = UUID.randomUUID();
        command.elderId = elderId;
        command.action = DeviceCommandAction.LOCK_AND_OPEN_MEMORIAL;
        command.status = DeviceCommandStatus.PENDING;
        command.attempts = 0;
        command.nextAttemptAt = now;
        command.createdAt = now;
        return command;
    }

    public boolean isRetryableAt(LocalDateTime now, int maxAttempts) {
        return status == DeviceCommandStatus.PENDING && attempts < maxAttempts
                && !nextAttemptAt.isAfter(now);
    }

    public void delivered(LocalDateTime now) {
        attempts++;
        status = DeviceCommandStatus.DELIVERED;
        deliveredAt = now;
        lastError = null;
    }

    public void failed(LocalDateTime now, String error) {
        attempts++;
        lastError = error == null ? "unknown device command failure" : error.substring(0, Math.min(500, error.length()));
        long backoffMinutes = Math.min(60, 1L << Math.min(attempts - 1, 6));
        nextAttemptAt = now.plusMinutes(backoffMinutes);
    }

    public void cancel() {
        if (status == DeviceCommandStatus.PENDING) {
            status = DeviceCommandStatus.CANCELLED;
        }
    }
}
