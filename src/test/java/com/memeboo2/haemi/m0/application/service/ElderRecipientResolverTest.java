package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ElderRecipientResolverTest {

    private final ElderRepository elders = mock(ElderRepository.class);
    private final ElderRecipientResolver resolver = new ElderRecipientResolver(elders);

    @Test
    void resolvesCanonicalElderProfileFromAlbumGroup() {
        UUID groupId = UUID.randomUUID();
        Elder elder = Elder.create(groupId, null, "김해미", 1940, Gender.FEMALE, ResidenceType.HOME_ALONE);
        when(elders.findByGroupId(groupId)).thenReturn(Optional.of(elder));

        assertThat(resolver.resolveByGroupId(groupId.toString())).contains(elder.getId().toString());

        verify(elders).findByGroupId(groupId);
    }

    @Test
    void rejectsLegacyNonUuidGroupIdentifierInsteadOfTreatingItAsAnElderAccount() {
        assertThat(resolver.resolveByGroupId("legacy-group")).isEmpty();

        verifyNoInteractions(elders);
    }
}
