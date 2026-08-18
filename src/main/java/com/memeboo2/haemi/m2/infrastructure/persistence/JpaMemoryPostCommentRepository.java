package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaMemoryPostCommentRepository extends JpaRepository<MemoryPostComment, UUID> {

    @Query("SELECT c FROM MemoryPostComment c WHERE c.postId = :postId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<MemoryPostComment> findActiveByPostId(@Param("postId") UUID postId);
}
