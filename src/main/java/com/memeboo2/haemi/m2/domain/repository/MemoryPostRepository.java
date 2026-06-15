package com.memeboo2.haemi.m2.domain.repository;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.PostStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryPostRepository {

    MemoryPost save(MemoryPost post);

    Optional<MemoryPost> findById(MemoryPostId id);

    // 공동 피드 최신순
    List<MemoryPost> findPublishedByAlbumIdOrderByPublishedAtDesc(UUID albumId, int offset, int limit);

    // 공동 피드 인기순
    List<MemoryPost> findPublishedByAlbumIdOrderByPopularityDesc(UUID albumId, int offset, int limit);

    // 임시 저장 목록
    List<MemoryPost> findDraftsByMemberId(String memberId);

    // 기간별 게시글 (랭킹 집계용)
    List<MemoryPost> findPublishedByAlbumIdAndPeriod(UUID albumId,
                                                      LocalDateTime from, LocalDateTime to);

    long countPublishedByAlbumId(UUID albumId);

    // 당일 어르신이 받은 알림 수 (F2-02 한도 확인용)
    int countTodayNotificationsSentToElder(UUID albumId);

    void delete(MemoryPost post);
}
