package com.memeboo2.haemi.m2.application.command;

import com.memeboo2.haemi.m2.domain.model.post.ReplyType;

import java.io.InputStream;

public record ReplyToPostCommand(
        String postId,
        String elderId,
        ReplyType replyType,
        String textContent,             // POEM, SHORT_TEXT 일 때
        InputStream voiceInputStream,   // STT 변환 원본 (선택)
        String voiceContentType,
        String imageKeyOrEmoji          // IMAGE 유형일 때
) {}
