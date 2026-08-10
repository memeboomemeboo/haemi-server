package com.memeboo2.haemi.m1.infrastructure.scheduler;

import com.memeboo2.haemi.m1.application.service.ReminiscenceApplicationService;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReminiscenceSchedulerTest {

    @Test
    void dailyRunLoadsEveryAlbum() {
        AlbumRepository albums = mock(AlbumRepository.class);
        ReminiscenceApplicationService reminiscence = mock(ReminiscenceApplicationService.class);
        Album first = Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Album second = Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(albums.findAll()).thenReturn(List.of(first, second));
        when(reminiscence.generateTodayReminiscence(anyString())).thenReturn(Optional.empty());

        new ReminiscenceScheduler(albums, reminiscence).generateDailyReminiscence();

        verify(albums).findAll();
        verify(albums, never()).findAllByElderProfileId(any());
        verify(reminiscence).generateTodayReminiscence(first.getId().toString());
        verify(reminiscence).generateTodayReminiscence(second.getId().toString());
    }
}
