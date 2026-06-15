package com.memeboo2.haemi.m2.domain.event;

import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemoryPostPublishedEvent(
        MemoryPostId postId,
        UUID albumId,
        AuthorInfo authorInfo,
        LocalDateTime occurredAt
) {}
