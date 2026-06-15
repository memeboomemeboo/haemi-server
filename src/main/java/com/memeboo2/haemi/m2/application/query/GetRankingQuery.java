package com.memeboo2.haemi.m2.application.query;

import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;

public record GetRankingQuery(String albumId, RankingPeriod period) {}
