package com.memeboo2.haemi.m5.domain.model.care;

import com.memeboo2.haemi.m5.domain.event.VoiceAlarmAcknowledgedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "voice_alarms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceAlarm extends AbstractAggregateRoot<VoiceAlarm> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false)
    private AlarmType alarmType;

    @Column(name = "alarm_time", nullable = false)
    private LocalTime alarmTime;

    @Column(name = "voice_key")
    private String voiceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_rule", nullable = false)
    private RepeatRule repeatRule;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "last_acknowledged_at")
    private LocalDateTime lastAcknowledgedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static VoiceAlarm create(String elderId, String groupId, AlarmType alarmType,
                                    LocalTime alarmTime, String voiceKey, RepeatRule repeatRule) {
        VoiceAlarm alarm = new VoiceAlarm();
        alarm.id = UUID.randomUUID();
        alarm.elderId = elderId;
        alarm.groupId = groupId;
        alarm.alarmType = alarmType;
        alarm.alarmTime = alarmTime;
        alarm.voiceKey = voiceKey;
        alarm.repeatRule = repeatRule;
        alarm.active = true;
        alarm.createdAt = LocalDateTime.now();
        return alarm;
    }

    public void markTriggered() {
        this.lastTriggeredAt = LocalDateTime.now();
    }

    public void acknowledge() {
        this.lastAcknowledgedAt = LocalDateTime.now();
        registerEvent(new VoiceAlarmAcknowledgedEvent(id, elderId, groupId, lastAcknowledgedAt));
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean usesTtsFallback() {
        return voiceKey == null || voiceKey.isBlank();
    }
}
