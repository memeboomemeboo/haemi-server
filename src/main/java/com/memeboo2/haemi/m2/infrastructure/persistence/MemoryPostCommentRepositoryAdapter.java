package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPostComment;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemoryPostCommentRepositoryAdapter implements MemoryPostCommentRepository {

    private final JpaMemoryPostCommentRepository jpa;

    @Override
    public MemoryPostComment save(MemoryPostComment comment) {
        return jpa.save(comment);
    }

    @Override
    public List<MemoryPostComment> findActiveByPostId(UUID postId) {
        return jpa.findActiveByPostId(postId);
    }

    @Override
    public Optional<MemoryPostComment> findById(UUID commentId) {
        return jpa.findById(commentId);
    }
}
