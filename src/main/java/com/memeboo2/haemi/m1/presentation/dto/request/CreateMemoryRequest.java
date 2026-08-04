package com.memeboo2.haemi.m1.presentation.dto.request;

import com.memeboo2.haemi.m1.domain.model.memory.MemoryVisibility;
import jakarta.validation.constraints.Size;

public record CreateMemoryRequest(
        @Size(max = 500, message = "글은 최대 500자까지 작성할 수 있어요.") String textContent,
        MemoryVisibility visibility
) {
}
