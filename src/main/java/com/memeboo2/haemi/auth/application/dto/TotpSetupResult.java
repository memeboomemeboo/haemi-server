package com.memeboo2.haemi.auth.application.dto;

public record TotpSetupResult(
        String secret,
        String qrUri
) {}
