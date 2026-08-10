package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;

import java.util.UUID;

public record CreateElderCommand(String orgId, String name, int birthYear, Gender gender,
                                 ResidenceType residenceType, UUID elderMemberId) {
}
