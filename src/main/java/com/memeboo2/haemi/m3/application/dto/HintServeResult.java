package com.memeboo2.haemi.m3.application.dto;

import com.memeboo2.haemi.m3.domain.model.hint.HintTier;

public record HintServeResult(
        TrainingSessionResult session,
        HintTier servedTier,
        String servedText
) {}
