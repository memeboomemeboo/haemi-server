package com.memeboo2.haemi.m2.application.command;

import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;

import java.time.LocalDate;

public record ComputeRankingCommand(
        String albumId,
        RankingPeriod period,
        LocalDate periodStart,
        LocalDate periodEnd
) {}
