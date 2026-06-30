package com.memeboo2.haemi.m2.domain.model.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderReply {

    @Enumerated(EnumType.STRING)
    @Column(name = "reply_type")
    private ReplyType replyType;

    // 시 내용(200자), 짧은 글(100자), 이미지 storageKey, 이모지 코드 등
    @Column(name = "reply_content", length = 300)
    private String content;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    private ElderReply(ReplyType replyType, String content, LocalDateTime repliedAt) {
        this.replyType = replyType;
        this.content = content;
        this.repliedAt = repliedAt;
    }

    public static ElderReply of(ReplyType replyType, String content) {
        validateContent(replyType, content);
        return new ElderReply(replyType, content, LocalDateTime.now());
    }

    private static void validateContent(ReplyType type, String content) {
        if (content == null || content.isBlank()) {
            throw new EmptyReplyContentException();
        }
        if (type == ReplyType.POEM && content.length() > 200) {
            throw new ReplyContentTooLongException("시는 최대 200자까지 가능합니다.");
        }
        if (type == ReplyType.SHORT_TEXT && content.length() > 100) {
            throw new ReplyContentTooLongException("짧은 글은 최대 100자까지 가능합니다.");
        }
    }
}
