package com.memeboo2.haemi.m2.application.command;

import com.memeboo2.haemi.m2.domain.model.post.ReplyType;

import java.io.InputStream;

public record ReplyToPostCommand(
        String postId,
        String elderId,
        ReplyType replyType,
        String heartEmojiCode,          // EMOJI 유형일 때 마음 이모지 코드
        InputStream voiceInputStream,   // VOICE 유형일 때 STT 변환 원본
        String voiceContentType
) {}
