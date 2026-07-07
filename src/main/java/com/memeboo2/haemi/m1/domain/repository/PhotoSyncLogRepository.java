package com.memeboo2.haemi.m1.domain.repository;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.PhotoSyncLog;

import java.util.List;

public interface PhotoSyncLogRepository {

    PhotoSyncLog save(PhotoSyncLog log);

    // 최신 30건만 반환 - 이력이 무한히 쌓여도 응답 크기를 제한한다
    List<PhotoSyncLog> findTop30ByAlbumIdOrderBySyncedAtDesc(AlbumId albumId);
}
