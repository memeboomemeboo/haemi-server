package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.PhotoSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPhotoSyncLogRepository extends JpaRepository<PhotoSyncLog, UUID> {

    List<PhotoSyncLog> findByAlbumIdOrderBySyncedAtDesc(UUID albumId);
}
