package com.memeboo2.haemi.m5.application.command;

public record CompleteWalkCommand(
        String walkRecordId,
        int durationMinutes,
        int stepCount
) {}
