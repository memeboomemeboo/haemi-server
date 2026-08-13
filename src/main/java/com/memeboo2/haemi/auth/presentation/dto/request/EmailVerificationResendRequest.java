package com.memeboo2.haemi.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationResendRequest(@NotBlank @Email String email) {}
