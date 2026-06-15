package com.memeboo2.haemi.m2.application.dto;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.PostStatus;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;

import java.time.LocalDateTime;
import java.util.List;

public record MemoryPostResult(
        String postId,
        String albumId,
        String authorMemberId,
        String authorName,
        String authorRelation,
        String textContent,
        List<String> photoKeys,
        String voiceMemoKey,
        PostStatus status,
        ElderReplyResult elderReply,
        int likeCount,
        int popularityScore,
        boolean readByElder,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
    public record ElderReplyResult(ReplyType replyType, String content, LocalDateTime repliedAt) {}

    public static MemoryPostResult from(MemoryPost post) {
        ElderReplyResult reply = null;
        if (post.getElderReply() != null) {
            reply = new ElderReplyResult(
                    post.getElderReply().getReplyType(),
                    post.getElderReply().getContent(),
                    post.getElderReply().getRepliedAt()
            );
        }
        return new MemoryPostResult(
                post.getId().toString(),
                post.getAlbumId().toString(),
                post.getAuthorInfo().getMemberId(),
                post.getAuthorInfo().getMemberName(),
                post.getAuthorInfo().getRelation(),
                post.getTextContent(),
                post.getPhotoKeys(),
                post.getVoiceMemoKey(),
                post.getStatus(),
                reply,
                post.getLikeCount(),
                post.getPopularityScore(),
                post.isReadByElder(),
                post.getPublishedAt(),
                post.getCreatedAt()
        );
    }
}
