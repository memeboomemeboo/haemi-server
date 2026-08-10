package com.memeboo2.haemi.m1.infrastructure.scheduler;

import com.memeboo2.haemi.m1.application.service.AlbumBatchScanner;
import com.memeboo2.haemi.m1.application.service.ReminiscenceApplicationService;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class ReminiscenceSchedulerTest {

    private final AlbumRepository albums = mock(AlbumRepository.class);
    private final ReminiscenceApplicationService reminiscence = mock(ReminiscenceApplicationService.class);
    private final ReminiscenceScheduler scheduler =
            new ReminiscenceScheduler(new AlbumBatchScanner(albums), reminiscence);

    @Test
    void dailyRunProcessesEveryAlbumPage() {
        Album first = album();
        Album second = album();
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(first, second));
        when(reminiscence.generateTodayReminiscence(anyString())).thenReturn(Optional.empty());

        scheduler.generateDailyReminiscence();

        verify(reminiscence).generateTodayReminiscence(first.getId().toString());
        verify(reminiscence).generateTodayReminiscence(second.getId().toString());
        // 마지막 페이지가 가득 차지 않았으면 다음 페이지를 요청하지 않는다.
        verify(albums).findPage(0, AlbumBatchScanner.BATCH_SIZE);
        verifyNoMoreInteractions(albums);
    }

    @Test
    void oneAlbumFailureDoesNotStopTheRest() {
        Album failing = album();
        Album healthy = album();
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(failing, healthy));
        when(reminiscence.generateTodayReminiscence(failing.getId().toString()))
                .thenThrow(new IllegalStateException("생성 실패"));
        when(reminiscence.generateTodayReminiscence(healthy.getId().toString()))
                .thenReturn(Optional.empty());

        scheduler.generateDailyReminiscence();

        verify(reminiscence).generateTodayReminiscence(healthy.getId().toString());
    }

    @Test
    void walksEveryPageUntilAPartialPageArrives() {
        List<Album> fullPage = fullPage();
        when(albums.findPage(0, AlbumBatchScanner.BATCH_SIZE)).thenReturn(fullPage);
        when(albums.findPage(1, AlbumBatchScanner.BATCH_SIZE)).thenReturn(List.of(album()));
        when(reminiscence.generateTodayReminiscence(anyString())).thenReturn(Optional.empty());

        scheduler.generateDailyReminiscence();

        verify(albums).findPage(0, AlbumBatchScanner.BATCH_SIZE);
        verify(albums).findPage(1, AlbumBatchScanner.BATCH_SIZE);
        verify(reminiscence, times(AlbumBatchScanner.BATCH_SIZE + 1)).generateTodayReminiscence(anyString());
    }

    private static List<Album> fullPage() {
        return java.util.stream.IntStream.range(0, AlbumBatchScanner.BATCH_SIZE)
                .mapToObj(index -> album())
                .toList();
    }

    private static Album album() {
        return Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }
}
