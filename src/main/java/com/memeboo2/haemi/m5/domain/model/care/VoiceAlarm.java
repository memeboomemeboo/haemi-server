package com.memeboo2.haemi.m5.domain.model.care;

import com.memeboo2.haemi.m5.domain.event.VoiceAlarmAcknowledgedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // 현재 재생 음성 (로테이션 풀에서 선택됨)
    @Column(name = "voice_key")
    private String voiceKey;

    // 가족 음성 로테이션 풀 (순서 보존)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "voice_alarm_voices",
            joinColumns = @JoinColumn(name = "alarm_id"))
    @OrderColumn(name = "voice_order")
    @Column(name = "voice_key")
    private List<String> voiceKeys = new ArrayList<>();

    @Column(name = "voice_rotation_index", nullable = false)
    private int voiceRotationIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_rule", nullable = false)
    private RepeatRule repeatRule;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "last_acknowledged_at")
    private LocalDateTime lastAcknowledgedAt;

    @Column(name = "last_no_response_notified_at")
    private LocalDateTime lastNoResponseNotifiedAt;

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
        if (voiceKey != null && !voiceKey.isBlank()) {
            alarm.voiceKeys.add(voiceKey);
        }
        alarm.voiceRotationIndex = 0;
        alarm.repeatRule = repeatRule;
        alarm.active = true;
        alarm.createdAt = LocalDateTime.now();
        return alarm;
    }

    // 로테이션 풀에 가족 음성을 추가한다. 첫 음성이면 현재 재생 음성으로 설정.
    public void addVoice(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        voiceKeys.add(key);
        if (voiceKeys.size() == 1) {
            this.voiceRotationIndex = 0;
            this.voiceKey = key;
        }
    }

    // 다음 음성으로 로테이션한다(풀이 2개 이상일 때만 순환). 현재 재생 음성을 갱신.
    public void rotateVoice() {
        if (voiceKeys.size() <= 1) {
            return;
        }
        this.voiceRotationIndex = (voiceRotationIndex + 1) % voiceKeys.size();
        this.voiceKey = voiceKeys.get(voiceRotationIndex);
    }

    // 발송 직전 상태 검증. 현재는 활성 여부만 확인한다.
    // 사별/입원 차단(EX-F501-06)·작고 가족 음성 차단(EX-F501-07)은 어르신 상태 머신(#36)
    // 의존이라 여기 앞단에 상태 조회 결과를 결합할 예정(#50).
    public boolean isDispatchable() {
        return active;
    }

    public List<String> getVoiceKeys() {
        return Collections.unmodifiableList(voiceKeys);
    }

    public int voiceCount() {
        return voiceKeys.size();
    }

    public void markTriggered() {
        markTriggered(LocalDateTime.now());
    }

    public void markTriggered(LocalDateTime triggeredAt) {
        this.lastTriggeredAt = triggeredAt;
        this.lastNoResponseNotifiedAt = null;
    }

    public void acknowledge() {
        acknowledge(LocalDateTime.now());
    }

    public void acknowledge(LocalDateTime acknowledgedAt) {
        if (!isAwaitingResponse()) {
            throw new AlarmNotAwaitingResponseException();
        }
        this.lastAcknowledgedAt = acknowledgedAt;
        registerEvent(new VoiceAlarmAcknowledgedEvent(id, elderId, groupId, lastAcknowledgedAt));
    }

    public boolean shouldTrigger(LocalDateTime now) {
        if (!active || !repeatRule.appliesTo(now.getDayOfWeek())) {
            return false;
        }
        if (alarmTime.getHour() != now.getHour() || alarmTime.getMinute() != now.getMinute()) {
            return false;
        }
        return lastTriggeredAt == null
                || !lastTriggeredAt.toLocalDate().equals(now.toLocalDate())
                || lastTriggeredAt.getHour() != now.getHour()
                || lastTriggeredAt.getMinute() != now.getMinute();
    }

    public boolean isNoResponseDue(LocalDateTime now) {
        return isAwaitingResponse()
                && !lastTriggeredAt.plusMinutes(10).isAfter(now)
                && (lastNoResponseNotifiedAt == null
                || lastNoResponseNotifiedAt.isBefore(lastTriggeredAt));
    }

    public void markNoResponseNotified(LocalDateTime notifiedAt) {
        if (!isNoResponseDue(notifiedAt)) {
            throw new AlarmNotAwaitingResponseException();
        }
        this.lastNoResponseNotifiedAt = notifiedAt;
    }

    public boolean isAwaitingResponse() {
        return lastTriggeredAt != null
                && (lastAcknowledgedAt == null || lastAcknowledgedAt.isBefore(lastTriggeredAt));
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean usesTtsFallback() {
        return voiceKey == null || voiceKey.isBlank();
    }
}
