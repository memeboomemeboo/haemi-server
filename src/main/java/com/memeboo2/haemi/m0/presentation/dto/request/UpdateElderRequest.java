package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.DiagnosisLevel;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateElderRequest(String orgId, String name, @Min(1920) @Max(1970) Integer birthYear,
                                 Gender gender, ResidenceType residenceType, DiagnosisLevel diagnosisLevel,
                                 String healthConsentId, LocalDate diagnosedAt, UUID elderMemberId) {
}
