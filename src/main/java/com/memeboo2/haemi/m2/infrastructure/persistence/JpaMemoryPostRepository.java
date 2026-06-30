package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaMemoryPostRepository extends JpaRepository<MemoryPost, UUID> {

    @Query("""
            SELECT p FROM MemoryPost p
            WHERE p.albumId = :albumId AND p.status = 'PUBLISHED'
            ORDER BY p.publishedAt DESC
            LIMIT :limit OFFSET :offset
            """)
    List<MemoryPost> findPublishedLatest(UUID albumId, int offset, int limit);

    @Query("""
            SELECT p FROM MemoryPost p
            WHERE p.albumId = :albumId AND p.status = 'PUBLISHED'
            ORDER BY p.popularityScore DESC, p.publishedAt DESC
            LIMIT :limit OFFSET :offset
            """)
    List<MemoryPost> findPublishedPopular(UUID albumId, int offset, int limit);

    @Query("""
            SELECT p FROM MemoryPost p
            WHERE p.albumId = :albumId AND p.status = 'PUBLISHED'
              AND p.publishedAt BETWEEN :from AND :to
            ORDER BY p.publishedAt ASC
            """)
    List<MemoryPost> findPublishedInPeriod(UUID albumId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT p FROM MemoryPost p WHERE p.authorInfo.memberId = :memberId AND p.status = 'DRAFT'")
    List<MemoryPost> findDraftsByMemberId(String memberId);

    long countByAlbumIdAndStatus(UUID albumId, PostStatus status);

    @Query("""
            SELECT COUNT(p) FROM MemoryPost p
            WHERE p.albumId = :albumId
              AND p.status = 'PUBLISHED'
              AND p.publishedAt >= :since
            """)
    int countPublishedSince(UUID albumId, LocalDateTime since);
}
