package com.memeboo2.haemi.m3.application.command;

public record AnswerTrainingQuestionCommand(
        String sessionId,
        String questionId,
        String submittedAnswer,
        int responseSeconds
) {}
