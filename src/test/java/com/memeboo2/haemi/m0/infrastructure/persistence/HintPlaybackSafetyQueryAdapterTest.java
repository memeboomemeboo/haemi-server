package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.Person;
import com.memeboo2.haemi.m0.domain.model.PersonLifeStatus;
import com.memeboo2.haemi.m0.domain.model.PersonVisibility;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.PersonRepository;
import com.memeboo2.haemi.m0.domain.repository.PhotoPersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HintPlaybackSafetyQueryAdapterTest {

    @Mock ElderRepository elders;
    @Mock PersonRepository persons;
    @Mock PhotoPersonRepository photoPersons;
    @InjectMocks HintPlaybackSafetyQueryAdapter safetyQuery;

    @Test
    void blocksMentionWhenAnyDuplicateNamedPersonIsHidden() {
        UUID groupId = UUID.randomUUID();
        Elder elder = Elder.create(groupId, null, "김어르신", 1940,
                Gender.FEMALE, ResidenceType.HOME_ALONE);
        Person visible = Person.create(groupId, "아드님", FamilyRelation.SON,
                PersonLifeStatus.ALIVE, null, PersonVisibility.SHOWN, null, null, null);
        Person hidden = Person.create(groupId, "아드님", FamilyRelation.SON,
                PersonLifeStatus.ALIVE, null, PersonVisibility.HIDDEN, null, null, null);
        given(elders.findById(elder.getId())).willReturn(Optional.of(elder));
        given(persons.findAllByGroupIdAndName(groupId, "아드님")).willReturn(List.of(visible, hidden));

        boolean playable = safetyQuery.isPlayable(elder.getId().toString(), null, null, "아드님");

        assertThat(playable).isFalse();
    }
}
