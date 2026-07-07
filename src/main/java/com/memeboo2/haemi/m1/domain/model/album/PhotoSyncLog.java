package com.memeboo2.haemi.m1.domain.model.album;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// F1-02: 동기화 이력 로그 (날짜별 동기화된 사진 수 확인용)
@Entity
@Table(name = "photo_sync_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoSyncLog {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "saved_count", nullable = false)
    private int savedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_type")
    private NetworkType networkType;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "background_sync", nullable = false)
    private boolean backgroundSync;

    public static PhotoSyncLog create(AlbumId albumId, String memberId, int requestedCount,
                                       int savedCount, int skippedCount,
                                       NetworkType networkType, Integer batteryLevel, boolean backgroundSync) {
        PhotoSyncLog log = new PhotoSyncLog();
        log.id = UUID.randomUUID();
        log.albumId = albumId.value();
        log.memberId = memberId;
        log.syncedAt = LocalDateTime.now();
        log.requestedCount = requestedCount;
        log.savedCount = savedCount;
        log.skippedCount = skippedCount;
        log.networkType = networkType;
        log.batteryLevel = batteryLevel;
        log.backgroundSync = backgroundSync;
        return log;
    }

    public AlbumId getAlbumId() {
        return AlbumId.of(albumId);
    }
}
