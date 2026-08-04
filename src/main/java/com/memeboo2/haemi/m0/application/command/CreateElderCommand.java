package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;

public record CreateElderCommand(String orgId, String name, int birthYear, Gender gender,
                                 ResidenceType residenceType) {
}
