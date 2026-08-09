package com.memeboo2.haemi.predownload.application;

import java.time.LocalDate;

public record PredownloadSummary(
        LocalDate date,
        int elderCount,
        int dispatchedCount,
        int totalAssets
) {}
