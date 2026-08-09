package com.memeboo2.haemi.m3.domain.model.hint;

public record ResolvedHint(
        HintTier tier,
        String text,
        String responderName
) {}
