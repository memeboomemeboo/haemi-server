package com.memeboo2.haemi.m1.application.dto;

import java.util.List;

public record MemoryFeedResult(List<MemoryResult> memories, long totalCount, int page, int size,
                               boolean hasNext, boolean deliveryAvailable) {
}
