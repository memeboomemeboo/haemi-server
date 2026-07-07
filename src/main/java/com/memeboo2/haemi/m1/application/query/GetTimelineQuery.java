package com.memeboo2.haemi.m1.application.query;

public record GetTimelineQuery(
        String albumId,
        String filterMemberId,
        String filterLocation,
        String filterTimePeriod,
        String sortBy,      // "SHOT_AT" | "UPLOADED_AT"
        String viewerRole   // "ELDER" | "FAMILY"
) {}
