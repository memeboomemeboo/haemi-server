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
}
