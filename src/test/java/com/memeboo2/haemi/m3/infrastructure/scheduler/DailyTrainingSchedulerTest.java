package com.memeboo2.haemi.m3.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.application.service.AlbumBatchScanner;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.PhotoFile;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class DailyTrainingSchedulerTest {

    private final NotificationPort notificationPort = mock(NotificationPort.class);
    private final AlbumRepository albumRepository = mock(AlbumRepository.class);
    private final ElderRecipientResolver elderRecipients = mock(ElderRecipientResolver.class);
    private final DailyTrainingScheduler scheduler = new DailyTrainingScheduler(
            notificationPort, new AlbumBatchScanner(albumRepository), elderRecipients);

    @Test
    void sendsReminderOnlyToProfilesWithAtLeastFivePhotos() {
        String eligibleElderId = UUID.randomUUID().toString();
        // 사진 수 조건은 쿼리가 거르므로, 저장소는 이미 걸러진 앨범만 돌려준다.
        when(albumRepository.findPageWithAtLeastPhotos(eq(5), eq(0), anyInt())).thenReturn(List.of(
                albumWithPhotos(eligibleElderId, 5),
                albumWithPhotos(eligibleElderId, 6)
        ));
        when(elderRecipients.resolveByGroupId("group-" + eligibleElderId))
                .thenReturn(Optional.of(eligibleElderId));

        scheduler.sendDailyTrainingReminder();

        // 같은 어르신의 앨범이 둘이어도 알림은 한 번만 간다.
        verify(notificationPort, times(1)).sendToElder(
                eligibleElderId,
                "오늘의 인지 훈련",
                "오늘의 훈련을 시작해볼까요?"
        );
    }

    @Test
    void skipsAlbumsWithoutLinkedElderProfile() {
        when(albumRepository.findPageWithAtLeastPhotos(eq(5), eq(0), anyInt()))
                .thenReturn(List.of(albumWithPhotos("not-ready-elder", 5)));
        when(elderRecipients.resolveByGroupId(anyString())).thenReturn(Optional.empty());

        scheduler.sendDailyTrainingReminder();

        verify(notificationPort, never()).sendToElder(anyString(), anyString(), anyString());
    }

    @Test
    void neverLoadsEveryAlbumAtOnce() {
        when(albumRepository.findPageWithAtLeastPhotos(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        scheduler.sendDailyTrainingReminder();

        verify(albumRepository).findPageWithAtLeastPhotos(5, 0, AlbumBatchScanner.BATCH_SIZE);
        verifyNoMoreInteractions(albumRepository);
    }

    private Album albumWithPhotos(String elderProfileId, int count) {
        Album album = Album.create(elderProfileId, "group-" + elderProfileId, "owner");
        for (int index = 0; index < count; index++) {
            album.addPhoto(
                    PhotoFile.of("key-" + index, "photo.jpg", "image/jpeg", 1024),
                    PhotoMetadata.of(LocalDateTime.now(), null, null),
                    elderProfileId + "-hash-" + index,
                    "owner"
            );
        }
        return album;
    }
}
