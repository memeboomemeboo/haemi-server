package com.memeboo2.haemi.m3.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
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

import static org.mockito.Mockito.*;

class DailyTrainingSchedulerTest {

    @Test
    void sendsReminderOnlyToProfilesWithAtLeastFivePhotos() {
        NotificationPort notificationPort = mock(NotificationPort.class);
        AlbumRepository albumRepository = mock(AlbumRepository.class);
        ElderRecipientResolver elderRecipients = mock(ElderRecipientResolver.class);
        String eligibleElderId = UUID.randomUUID().toString();
        DailyTrainingScheduler scheduler =
                new DailyTrainingScheduler(notificationPort, albumRepository, elderRecipients);
        when(albumRepository.findAll()).thenReturn(List.of(
                albumWithPhotos(eligibleElderId, 5),
                albumWithPhotos("not-ready-elder", 4),
                albumWithPhotos(eligibleElderId, 6)
        ));
        when(elderRecipients.resolveByGroupId("group-" + eligibleElderId))
                .thenReturn(Optional.of(eligibleElderId));

        scheduler.sendDailyTrainingReminder();

        verify(notificationPort, times(1)).sendToElder(
                eligibleElderId,
                "오늘의 인지 훈련",
                "오늘의 훈련을 시작해볼까요?"
        );
        verify(notificationPort, never()).sendToElder(
                eq("not-ready-elder"),
                anyString(),
                anyString()
        );
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
