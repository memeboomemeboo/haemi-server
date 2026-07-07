package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.album.NetworkType;
import com.memeboo2.haemi.m1.domain.model.album.PhotoSyncLog;

import java.time.LocalDateTime;

public record SyncHistoryResult(
        String memberId,
        LocalDateTime syncedAt,
        int requestedCount,
        int savedCount,
        int skippedCount,
        NetworkType networkType,
        Integer batteryLevel,
        boolean backgroundSync
) {
    public static SyncHistoryResult from(PhotoSyncLog log) {
        return new SyncHistoryResult(
                log.getMemberId(),
                log.getSyncedAt(),
                log.getRequestedCount(),
                log.getSavedCount(),
                log.getSkippedCount(),
                log.getNetworkType(),
                log.getBatteryLevel(),
                log.isBackgroundSync()
        );
    }
}
