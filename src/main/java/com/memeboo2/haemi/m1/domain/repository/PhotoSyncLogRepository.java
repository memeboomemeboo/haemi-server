package com.memeboo2.haemi.m1.domain.repository;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.PhotoSyncLog;

import java.util.List;

public interface PhotoSyncLogRepository {

    PhotoSyncLog save(PhotoSyncLog log);

    List<PhotoSyncLog> findByAlbumIdOrderBySyncedAtDesc(AlbumId albumId);
}
