package com.memeboo2.haemi.m0.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersonSafetyTest {

    @Test
    void deceasedPersonIsLimitedToPastTenseAndHiddenPersonIsExcluded() {
        Person person = Person.create(UUID.randomUUID(), "할아버지", FamilyRelation.SPOUSE,
                PersonLifeStatus.DECEASED, LocalDate.of(2020, 1, 1), PersonVisibility.SHOWN,
                null, null, null);

        assertThat(person.contentTense()).isEqualTo(PersonContentTense.PAST_ONLY);

        person.update(null, null, PersonVisibility.HIDDEN, null, null);

        assertThat(person.contentTense()).isEqualTo(PersonContentTense.EXCLUDED);
    }

    @Test
    void unknownPersonCannotBeNamedInGeneratedContent() {
        Person person = Person.create(UUID.randomUUID(), "이웃분", FamilyRelation.FRIEND,
                PersonLifeStatus.UNKNOWN, null, PersonVisibility.SHOWN, null, null, null);

        assertThat(person.contentTense()).isEqualTo(PersonContentTense.NEUTRAL_ONLY);
    }
}
