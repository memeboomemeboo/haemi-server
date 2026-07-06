package com.memeboo2.haemi.m5.domain.model.care;

import com.memeboo2.haemi.m5.domain.event.WalkCompletedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "walk_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkRecord extends AbstractAggregateRoot<WalkRecord> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "routine_id", columnDefinition = "uuid", nullable = false)
    private UUID routineId;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "walk_date", nullable = false)
    private LocalDate walkDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WalkStatus status;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "step_count", nullable = false)
    private int stepCount;

    @Column(name = "weather_condition")
    @Enumerated(EnumType.STRING)
    private WeatherCondition weatherCondition;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static WalkRecord start(WalkRoutine routine, WeatherCondition weatherCondition) {
        WalkRecord record = new WalkRecord();
        record.id = UUID.randomUUID();
        record.routineId = routine.getId();
        record.elderId = routine.getElderId();
        record.groupId = routine.getGroupId();
        record.walkDate = LocalDate.now();
        record.status = isSevere(weatherCondition) ? WalkStatus.CANCELLED_BY_WEATHER : WalkStatus.STARTED;
        record.weatherCondition = weatherCondition;
        record.startedAt = LocalDateTime.now();
        return record;
    }

    public void complete(int durationMinutes, int stepCount) {
        if (status != WalkStatus.STARTED) {
            throw new WalkCompletionUnavailableException();
        }
        this.durationMinutes = Math.max(durationMinutes, 0);
        this.stepCount = Math.max(stepCount, 0);
        this.status = WalkStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        registerEvent(new WalkCompletedEvent(id, elderId, groupId, this.durationMinutes, this.stepCount, completedAt));
    }

    private static boolean isSevere(WeatherCondition condition) {
        return condition == WeatherCondition.RAIN
                || condition == WeatherCondition.HEAVY_SNOW
                || condition == WeatherCondition.HEAT_WAVE;
    }
}
