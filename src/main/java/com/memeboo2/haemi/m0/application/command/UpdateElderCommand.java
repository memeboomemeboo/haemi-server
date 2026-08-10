package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.DiagnosisLevel;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateElderCommand(String orgId, String name, Integer birthYear, Gender gender,
                                 ResidenceType residenceType, DiagnosisLevel diagnosisLevel,
                                 String healthConsentId, LocalDate diagnosedAt, UUID elderMemberId) {
}
