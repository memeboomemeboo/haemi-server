package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.LifeStoryCategory;
import com.memeboo2.haemi.m0.domain.model.LifeStorySource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LifeStoryItemRequest(@NotNull LifeStoryCategory category, @NotBlank String value,
                                   Integer weight, LifeStorySource source) {
}
