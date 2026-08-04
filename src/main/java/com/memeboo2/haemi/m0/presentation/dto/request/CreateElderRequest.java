package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateElderRequest(String orgId, @NotBlank String name, @Min(1920) @Max(1970) int birthYear,
                                 @NotNull Gender gender, @NotNull ResidenceType residenceType) {
}
