package com.memeboo2.haemi.m2.domain.event;

import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ElderRepliedEvent(
        MemoryPostId postId,
        UUID albumId,
        ReplyType replyType,
        LocalDateTime occurredAt
) {}
