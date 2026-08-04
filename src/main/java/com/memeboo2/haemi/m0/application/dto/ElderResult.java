package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;

import java.util.UUID;

public record ElderResult(UUID elderId, UUID groupId, String name, int birthYear, Gender gender,
                          ResidenceType residenceType, ElderAccessMode accessMode, ElderStatus status,
                          int personalizationLevel, boolean hasHealthInformation, double completeness) {
    public static ElderResult from(Elder elder, boolean hasHealthInformation, double completeness) {
        return new ElderResult(elder.getId(), elder.getGroupId(), elder.getName(), elder.getBirthYear(),
                elder.getGender(), elder.getResidenceType(), elder.getAccessMode(), elder.getStatus(),
                elder.getPersonalizationLevel(), hasHealthInformation, completeness);
    }
}
