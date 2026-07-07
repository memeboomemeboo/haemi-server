package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.application.command.SavePhotoCommand;
import com.memeboo2.haemi.m1.application.command.SyncPhotosCommand;
import com.memeboo2.haemi.m1.application.dto.SyncHistoryResult;
import com.memeboo2.haemi.m1.application.query.GetSyncHistoryQuery;
import com.memeboo2.haemi.m1.domain.model.album.*;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.PhotoSyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoApplicationServiceTest {

    @Mock AlbumRepository albumRepository;
    @Mock PhotoStoragePort photoStoragePort;
    @Mock NotificationPort notificationPort;
    @Mock PhotoSyncLogRepository photoSyncLogRepository;

    PhotoApplicationService service;
    Album album;

    @BeforeEach
    void setUp() {
        service = new PhotoApplicationService(albumRepository, photoStoragePort, notificationPort, photoSyncLogRepository);
        album = Album.create("elder-1", "group-1", "owner");
        when(albumRepository.findById(album.getAlbumId())).thenReturn(Optional.of(album));
    }

    @Test
    @DisplayName("Wi-Fi 전용 설정 상태에서 셀룰러 연결이면 동기화를 거부한다")
    void syncPhotos_rejectsCellularWhenWifiOnly() {
        SyncPhotosCommand command = new SyncPhotosCommand(
                album.getAlbumId().toString(), "owner", List.of(),
                true, NetworkType.CELLULAR, null, false);

        assertThatThrownBy(() -> service.syncPhotos(command))
                .isInstanceOf(SyncConditionNotMetException.class);
        verifyNoInteractions(photoSyncLogRepository);
    }

    @Test
    @DisplayName("배터리 20% 이하이면 동기화를 거부한다")
    void syncPhotos_rejectsLowBattery() {
        SyncPhotosCommand command = new SyncPhotosCommand(
                album.getAlbumId().toString(), "owner", List.of(),
                false, NetworkType.WIFI, 20, false);

        assertThatThrownBy(() -> service.syncPhotos(command))
                .isInstanceOf(SyncConditionNotMetException.class);
        verifyNoInteractions(photoSyncLogRepository);
    }

    @Test
    @DisplayName("동기화 성공 시 날짜별 이력을 기록한다")
    void syncPhotos_recordsSyncHistory() throws Exception {
        when(photoStoragePort.store(any(), any(), any())).thenReturn("storage-key");
        SavePhotoCommand photoCmd = new SavePhotoCommand(
                album.getAlbumId().toString(), "owner",
                new ByteArrayInputStream("data".getBytes()), "a.jpg", "image/jpeg", 10,
                "hash-1", null, null, null);
        SyncPhotosCommand command = new SyncPhotosCommand(
                album.getAlbumId().toString(), "owner", List.of(photoCmd),
                true, NetworkType.WIFI, 80, true);

        service.syncPhotos(command);

        ArgumentCaptor<PhotoSyncLog> captor = ArgumentCaptor.forClass(PhotoSyncLog.class);
        verify(photoSyncLogRepository).save(captor.capture());
        PhotoSyncLog savedLog = captor.getValue();
        assertThat(savedLog.getSavedCount()).isEqualTo(1);
        assertThat(savedLog.getSkippedCount()).isEqualTo(0);
        assertThat(savedLog.getNetworkType()).isEqualTo(NetworkType.WIFI);
        assertThat(savedLog.getBatteryLevel()).isEqualTo(80);
        assertThat(savedLog.isBackgroundSync()).isTrue();
    }

    @Test
    @DisplayName("동기화 이력을 최신순으로 조회한다")
    void getSyncHistory_returnsLogs() {
        PhotoSyncLog logEntry = PhotoSyncLog.create(album.getAlbumId(), "owner", 1, 1, 0,
                NetworkType.WIFI, 90, false);
        when(photoSyncLogRepository.findByAlbumIdOrderBySyncedAtDesc(album.getAlbumId()))
                .thenReturn(List.of(logEntry));

        List<SyncHistoryResult> result = service.getSyncHistory(
                new GetSyncHistoryQuery(album.getAlbumId().toString()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).savedCount()).isEqualTo(1);
    }
}
