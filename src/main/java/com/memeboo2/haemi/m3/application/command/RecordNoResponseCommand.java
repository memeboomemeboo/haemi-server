package com.memeboo2.haemi.m3.application.command;

public record RecordNoResponseCommand(
        String sessionId,
        String questionId
) {}
