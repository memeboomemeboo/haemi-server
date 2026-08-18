package com.memeboo2.haemi.m2.domain.model.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memory_post_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryPostComment {

    private static final int MAX_CONTENT_LEN = 200;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "post_id", nullable = false, columnDefinition = "uuid")
    private UUID postId;

    @Column(name = "member_id", nullable = false, length = 100)
    private String memberId;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "relation", nullable = false, length = 30)
    private String relation;

    @Column(name = "content", nullable = false, length = 200)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static MemoryPostComment create(UUID postId, String memberId, String memberName,
                                           String relation, String content) {
        if (content == null || content.isBlank()) {
            throw new CommentContentRequiredException();
        }
        if (content.length() > MAX_CONTENT_LEN) {
            throw new CommentContentTooLongException(content.length());
        }
        MemoryPostComment c = new MemoryPostComment();
        c.id = UUID.randomUUID();
        c.postId = postId;
        c.memberId = memberId;
        c.memberName = memberName;
        c.relation = relation;
        c.content = content.trim();
        c.createdAt = LocalDateTime.now();
        return c;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete(String requestingMemberId) {
        if (!memberId.equals(requestingMemberId)) {
            throw new CommentDeleteForbiddenException();
        }
        this.deletedAt = LocalDateTime.now();
    }
}
