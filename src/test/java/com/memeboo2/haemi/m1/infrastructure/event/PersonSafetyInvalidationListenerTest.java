package com.memeboo2.haemi.m1.infrastructure.event;

import com.memeboo2.haemi.m0.domain.event.PersonSafetyChangedEvent;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.ReminiscenceContentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class PersonSafetyInvalidationListenerTest {

    @Test
    void hiddenPersonImmediatelyInvalidatesExistingGroupCards() {
        AlbumRepository albums = mock(AlbumRepository.class);
        ReminiscenceContentRepository contents = mock(ReminiscenceContentRepository.class);
        UUID groupId = UUID.randomUUID();
        Album album = Album.create(UUID.randomUUID().toString(), groupId.toString(), UUID.randomUUID().toString());
        when(albums.findByGroupId(groupId.toString())).thenReturn(Optional.of(album));

        new PersonSafetyInvalidationListener(albums, contents)
                .invalidate(new PersonSafetyChangedEvent(groupId, UUID.randomUUID()));

        verify(contents).invalidateByAlbumId(album.getAlbumId());
    }
}
