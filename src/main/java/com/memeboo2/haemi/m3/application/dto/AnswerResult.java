package com.memeboo2.haemi.m3.application.dto;

public record AnswerResult(
        TrainingSessionResult session,
        boolean correct,
        String message
) {}
