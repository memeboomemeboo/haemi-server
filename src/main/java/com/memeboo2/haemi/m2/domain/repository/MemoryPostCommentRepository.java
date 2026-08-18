package com.memeboo2.haemi.m2.domain.repository;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPostComment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryPostCommentRepository {
    MemoryPostComment save(MemoryPostComment comment);
    List<MemoryPostComment> findActiveByPostId(UUID postId);
    Optional<MemoryPostComment> findById(UUID commentId);
}
