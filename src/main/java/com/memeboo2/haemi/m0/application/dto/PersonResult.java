package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.Person;
import com.memeboo2.haemi.m0.domain.model.PersonContentTense;
import com.memeboo2.haemi.m0.domain.model.PersonLifeStatus;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;

import java.time.LocalDate;
import java.util.UUID;

public record PersonResult(UUID personId, UUID groupId, String name, FamilyRelation relation,
                           PersonLifeStatus lifeStatus, LocalDate deceasedAt, PersonVisibility visibility,
                           String nickname, boolean active, PersonContentTense contentTense) {
    public static PersonResult from(Person person) {
        return new PersonResult(person.getId(), person.getGroupId(), person.getName(), person.getRelation(),
                person.getLifeStatus(), person.getDeceasedAt(), person.getVisibility(), person.getNickname(),
                person.isActive(), person.contentTense());
    }
}
