package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSensitiveTopicRequest(@NotBlank @Size(max = 100) String keyword,
                                          @Size(max = 300) String reason) {
}
