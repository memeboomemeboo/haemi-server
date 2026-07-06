package com.memeboo2.haemi.m3.presentation.dto.request;

import com.memeboo2.haemi.m3.domain.model.training.QuestionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Set;

public record UpdateDifficultyPolicyRequest(
        @Positive double maxAverageResponseSeconds,
        @DecimalMin("0.0") @DecimalMax("1.0") double increaseAccuracyThreshold,
        @DecimalMin("0.0") @DecimalMax("1.0") double decreaseAccuracyThreshold,
        @NotEmpty Set<QuestionType> questionTypes,
        LocalDate reviewedDate
) {
}
