package com.memeboo2.haemi.m5.application.dto;

import com.memeboo2.haemi.m5.domain.model.care.AlarmType;
import com.memeboo2.haemi.m5.domain.model.care.RepeatRule;
import com.memeboo2.haemi.m5.domain.model.care.VoiceAlarm;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record VoiceAlarmResult(
        String alarmId,
        String elderId,
        String groupId,
        AlarmType alarmType,
        LocalTime alarmTime,
        RepeatRule repeatRule,
        String voiceKey,
        int voiceCount,
        boolean ttsFallback,
        boolean active,
        LocalDateTime lastTriggeredAt,
        LocalDateTime lastAcknowledgedAt,
        LocalDateTime lastNoResponseNotifiedAt,
        boolean awaitingResponse
) {
    public static VoiceAlarmResult from(VoiceAlarm alarm) {
        return new VoiceAlarmResult(
                alarm.getId().toString(),
                alarm.getElderId(),
                alarm.getGroupId(),
                alarm.getAlarmType(),
                alarm.getAlarmTime(),
                alarm.getRepeatRule(),
                alarm.getVoiceKey(),
                alarm.voiceCount(),
                alarm.usesTtsFallback(),
                alarm.isActive(),
                alarm.getLastTriggeredAt(),
                alarm.getLastAcknowledgedAt(),
                alarm.getLastNoResponseNotifiedAt(),
                alarm.isAwaitingResponse()
        );
    }
}
