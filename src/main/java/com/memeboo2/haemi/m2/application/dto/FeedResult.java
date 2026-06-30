package com.memeboo2.haemi.m2.application.dto;

import java.util.List;

public record FeedResult(
        List<MemoryPostResult> posts,
        long totalCount,
        int page,
        int size,
        boolean hasNext
) {}
