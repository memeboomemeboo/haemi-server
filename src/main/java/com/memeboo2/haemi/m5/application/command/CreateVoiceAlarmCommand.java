package com.memeboo2.haemi.m5.application.command;

import com.memeboo2.haemi.m5.domain.model.care.AlarmType;
import com.memeboo2.haemi.m5.domain.model.care.RepeatRule;

import java.io.InputStream;
import java.time.LocalTime;

public record CreateVoiceAlarmCommand(
        String elderId,
        String groupId,
        AlarmType alarmType,
        LocalTime alarmTime,
        RepeatRule repeatRule,
        InputStream voiceInputStream,
        String voiceFilename,
        String voiceContentType
) {}
