package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.PersonLifeStatus;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;

import java.time.LocalDate;
import java.util.UUID;

public record UpdatePersonCommand(FamilyRelation relation, PersonLifeStatus lifeStatus, LocalDate deceasedAt,
                                  PersonVisibility visibility, String nickname, UUID profilePhotoId) {
}
