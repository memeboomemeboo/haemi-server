package com.memeboo2.haemi.m2.application.query;

public record GetFeedQuery(
        String albumId,
        String sortBy,  // "LATEST" | "POPULAR"
        String period,  // "ALL" | "WEEKLY" | "MONTHLY"
        int page,
        int size
) {}
