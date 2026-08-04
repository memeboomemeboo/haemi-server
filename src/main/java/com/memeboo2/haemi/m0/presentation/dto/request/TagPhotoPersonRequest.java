package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TagPhotoPersonRequest(@NotNull UUID personId,
                                    @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
                                    boolean confirmed) {
}
