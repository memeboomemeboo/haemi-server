package com.memeboo2.haemi.m3.application.command;

import com.memeboo2.haemi.m3.domain.model.hint.AccrualSource;

import java.util.UUID;

public record AccrueHintCommand(
        String elderId,
        UUID photoId,
        String personName,
        AccrualSource source,
        String authorMemberId,
        String authorName,
        String text
) {}
