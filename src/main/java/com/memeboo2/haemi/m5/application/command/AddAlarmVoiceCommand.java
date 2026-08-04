package com.memeboo2.haemi.m5.application.command;

import java.io.InputStream;

public record AddAlarmVoiceCommand(
        String alarmId,
        String elderId,
        InputStream voiceInputStream,
        String voiceFilename,
        String voiceContentType
) {}
