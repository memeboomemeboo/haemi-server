package com.memeboo2.haemi.m2.application.dto;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPostComment;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemoryPostCommentResult(
        UUID commentId,
        UUID postId,
        String memberId,
        String memberName,
        String relation,
        String content,
        LocalDateTime createdAt
) {
    public static MemoryPostCommentResult from(MemoryPostComment comment) {
        return new MemoryPostCommentResult(
                comment.getId(),
                comment.getPostId(),
                comment.getMemberId(),
                comment.getMemberName(),
                comment.getRelation(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
