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

    // VOICE: STT 전사 텍스트(최대 300자), EMOJI: 마음 이모지 코드
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

    private static final int MAX_VOICE_LEN = 300;

    private static void validateContent(ReplyType type, String content) {
        if (content == null || content.isBlank()) {
            throw new EmptyReplyContentException();
        }
        if (type == ReplyType.VOICE && content.length() > MAX_VOICE_LEN) {
            throw new ReplyContentTooLongException("음성 답변은 최대 300자까지 가능합니다.");
        }
        if (type == ReplyType.EMOJI && !HeartEmoji.isValidCode(content)) {
            throw new InvalidHeartEmojiException(content);
        }
    }
}
