package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.PersonLifeStatus;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePersonRequest(@NotBlank String name, @NotNull FamilyRelation relation,
                                  @NotNull PersonLifeStatus lifeStatus, LocalDate deceasedAt,
                                  PersonVisibility visibility, String nickname, UUID profilePhotoId,
                                  UUID linkedMemberId) {
}
