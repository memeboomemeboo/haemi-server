package com.memeboo2.haemi.m0.application.dto;

public record ElderCompletenessResult(double score, PersonalizationReadiness readiness) {
    public enum PersonalizationReadiness {
        ONBOARDING_REQUIRED,
        BASIC,
        FULL
    }

    public static ElderCompletenessResult from(double score) {
        PersonalizationReadiness readiness = score < 0.3 ? PersonalizationReadiness.ONBOARDING_REQUIRED
                : score < 0.6 ? PersonalizationReadiness.BASIC : PersonalizationReadiness.FULL;
        return new ElderCompletenessResult(score, readiness);
    }
}
