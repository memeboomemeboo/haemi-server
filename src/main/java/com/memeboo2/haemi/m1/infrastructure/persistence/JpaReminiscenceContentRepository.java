package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaReminiscenceContentRepository extends JpaRepository<ReminiscenceContent, UUID> {

    Optional<ReminiscenceContent> findByAlbumIdAndGeneratedDate(UUID albumId, LocalDate generatedDate);

    @Query("SELECT rc FROM ReminiscenceContent rc WHERE rc.albumId = :albumId ORDER BY rc.generatedAt DESC")
    List<ReminiscenceContent> findRecentByAlbumId(UUID albumId, int limit);

    void deleteAllByAlbumId(UUID albumId);

    @Query("""
            SELECT DISTINCT s.photoId
            FROM ReminiscenceContent rc
            JOIN rc.slideCards s
            WHERE rc.albumId = :albumId
              AND rc.generatedDate >= :since
            """)
    List<UUID> findRecentlyUsedPhotoIds(UUID albumId, LocalDate since);

}
