package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.PostStatus;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemoryPostRepositoryAdapter implements MemoryPostRepository {

    private final JpaMemoryPostRepository jpa;

    @Override
    public MemoryPost save(MemoryPost post) {
        return jpa.save(post);
    }

    @Override
    public Optional<MemoryPost> findById(MemoryPostId id) {
        return jpa.findById(id.value());
    }

    @Override
    public List<MemoryPost> findPublishedByAlbumIdOrderByPublishedAtDesc(UUID albumId, int offset, int limit) {
        return jpa.findPublishedLatest(albumId, offset, limit);
    }

    @Override
    public List<MemoryPost> findPublishedByAlbumIdOrderByPopularityDesc(UUID albumId, int offset, int limit) {
        return jpa.findPublishedPopular(albumId, offset, limit);
    }

    @Override
    public List<MemoryPost> findDraftsByMemberId(String memberId) {
        return jpa.findDraftsByMemberId(memberId);
    }

    @Override
    public List<MemoryPost> findPublishedByAlbumIdAndPeriod(UUID albumId, LocalDateTime from, LocalDateTime to) {
        return jpa.findPublishedInPeriod(albumId, from, to);
    }

    @Override
    public long countPublishedByAlbumId(UUID albumId) {
        return jpa.countByAlbumIdAndStatus(albumId, PostStatus.PUBLISHED);
    }

    @Override
    public int countTodayNotificationsSentToElder(UUID albumId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return jpa.countPublishedSince(albumId, startOfDay);
    }

    @Override
    public boolean existsUnreadPublishedByAlbumIdSince(UUID albumId, LocalDateTime since) {
        return jpa.existsByAlbumIdAndStatusAndReadByElderFalseAndPublishedAtGreaterThanEqual(
                albumId, PostStatus.PUBLISHED, since);
    }

    @Override
    public void delete(MemoryPost post) {
        jpa.save(post); // soft delete (status = DELETED)
    }
}
