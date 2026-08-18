package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m2.application.dto.MemoryPostCommentResult;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostComment;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostNotFoundException;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostCommentRepository;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoryPostCommentService {

    private final MemoryPostCommentRepository comments;
    private final MemoryPostRepository posts;

    @Transactional
    public MemoryPostCommentResult addComment(String postId, String memberId, String memberName,
                                              String relation, String content) {
        UUID postUuid = parse(postId);
        posts.findById(MemoryPostId.of(postUuid)).orElseThrow(() -> new MemoryPostNotFoundException(postId));
        MemoryPostComment comment = MemoryPostComment.create(postUuid, memberId, memberName, relation, content);
        return MemoryPostCommentResult.from(comments.save(comment));
    }

    @Transactional(readOnly = true)
    public List<MemoryPostCommentResult> listComments(String postId) {
        return comments.findActiveByPostId(parse(postId))
                .stream()
                .map(MemoryPostCommentResult::from)
                .toList();
    }

    @Transactional
    public void deleteComment(String commentId, String requestingMemberId) {
        MemoryPostComment comment = comments.findById(UUID.fromString(commentId))
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        comment.delete(requestingMemberId);
        comments.save(comment);
    }

    private UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new MemoryPostNotFoundException(id);
        }
    }
}
