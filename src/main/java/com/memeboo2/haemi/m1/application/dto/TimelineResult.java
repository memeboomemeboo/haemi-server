package com.memeboo2.haemi.m1.application.dto;

import java.util.List;

public record TimelineResult(
        String albumId,
        List<TimelineGroup> groups,
        int totalCount
) {
    public record TimelineGroup(
            String period,          // "2024년 봄", "날짜 미상" 등
            List<PhotoResult> photos
    ) {}
}
