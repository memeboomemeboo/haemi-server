package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.PhotoSyncLog;
import com.memeboo2.haemi.m1.domain.repository.PhotoSyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PhotoSyncLogRepositoryAdapter implements PhotoSyncLogRepository {

    private final JpaPhotoSyncLogRepository jpa;

    @Override
    public PhotoSyncLog save(PhotoSyncLog log) {
        return jpa.save(log);
    }

    @Override
    public List<PhotoSyncLog> findByAlbumIdOrderBySyncedAtDesc(AlbumId albumId) {
        return jpa.findByAlbumIdOrderBySyncedAtDesc(albumId.value());
    }
}
