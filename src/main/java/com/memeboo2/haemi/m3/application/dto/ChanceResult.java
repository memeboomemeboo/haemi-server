package com.memeboo2.haemi.m3.application.dto;

public record ChanceResult(
        String sessionId,
        int remainingChanceCount,
        String message
) {}
