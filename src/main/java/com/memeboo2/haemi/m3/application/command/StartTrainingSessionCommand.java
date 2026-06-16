package com.memeboo2.haemi.m3.application.command;

import com.memeboo2.haemi.m3.domain.model.training.StartMode;

public record StartTrainingSessionCommand(
        String elderId,
        String albumId,
        StartMode startMode
) {}
