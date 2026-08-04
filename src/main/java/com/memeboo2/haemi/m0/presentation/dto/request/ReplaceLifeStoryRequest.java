package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceLifeStoryRequest(@NotNull List<@Valid LifeStoryItemRequest> items) {
}
