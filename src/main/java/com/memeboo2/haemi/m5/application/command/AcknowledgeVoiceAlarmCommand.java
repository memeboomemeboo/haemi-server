package com.memeboo2.haemi.m5.application.command;

public record AcknowledgeVoiceAlarmCommand(
        String alarmId,
        String elderId
) {}
