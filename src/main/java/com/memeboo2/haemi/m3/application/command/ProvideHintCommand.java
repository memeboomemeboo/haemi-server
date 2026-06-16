package com.memeboo2.haemi.m3.application.command;

public record ProvideHintCommand(
        String sessionId,
        String responderMemberId,
        String responderName,
        String hintText
) {}
