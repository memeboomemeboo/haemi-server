package com.memeboo2.haemi.m5.domain.model.care;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "walk_routines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkRoutine {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "morning_time")
    private LocalTime morningTime;

    @Column(name = "afternoon_time")
    private LocalTime afternoonTime;

    @Column(name = "target_minutes", nullable = false)
    private int targetMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_reminded_at")
    private LocalDateTime lastRemindedAt;

    public static WalkRoutine create(String elderId, String groupId, LocalTime morningTime,
                                     LocalTime afternoonTime, int targetMinutes) {
        WalkRoutine routine = new WalkRoutine();
        routine.id = UUID.randomUUID();
        routine.elderId = elderId;
        routine.groupId = groupId;
        routine.morningTime = morningTime;
        routine.afternoonTime = afternoonTime;
        routine.targetMinutes = targetMinutes <= 0 ? 10 : targetMinutes;
        routine.active = true;
        routine.createdAt = LocalDateTime.now();
        return routine;
    }

    public boolean shouldRemind(LocalDateTime now) {
        if (!active) {
            return false;
        }
        boolean due = matches(now, morningTime) || matches(now, afternoonTime);
        if (!due) {
            return false;
        }
        return lastRemindedAt == null
                || !lastRemindedAt.toLocalDate().equals(now.toLocalDate())
                || lastRemindedAt.getHour() != now.getHour()
                || lastRemindedAt.getMinute() != now.getMinute();
    }

    public void markReminded(LocalDateTime remindedAt) {
        if (!shouldRemind(remindedAt)) {
            throw new IllegalStateException("현재 시각에는 산책 알림을 보낼 수 없습니다.");
        }
        this.lastRemindedAt = remindedAt;
    }

    private boolean matches(LocalDateTime now, LocalTime reminderTime) {
        return reminderTime != null
                && reminderTime.getHour() == now.getHour()
                && reminderTime.getMinute() == now.getMinute();
    }
}
