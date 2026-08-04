package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OwnershipTransferRequest(@NotNull UUID recipientMemberId) {
}
