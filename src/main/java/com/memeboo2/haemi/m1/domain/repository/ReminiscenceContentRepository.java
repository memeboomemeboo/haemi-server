package com.memeboo2.haemi.m1.domain.repository;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContent;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContentId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReminiscenceContentRepository {

    ReminiscenceContent save(ReminiscenceContent content);

    Optional<ReminiscenceContent> findById(ReminiscenceContentId id);

    Optional<ReminiscenceContent> findByAlbumIdAndDate(AlbumId albumId, LocalDate date);

    // 최근 N일 이내 노출된 photoId 목록 조회 (로테이션 관리용)
    Set<UUID> findRecentlyUsedPhotoIds(AlbumId albumId, int days);

    List<ReminiscenceContent> findRecentByAlbumId(AlbumId albumId, int limit);
}
