package com.memeboo2.haemi.m2.domain.repository;

import com.memeboo2.haemi.m2.domain.model.ranking.FamilyRanking;
import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FamilyRankingRepository {

    FamilyRanking save(FamilyRanking ranking);

    Optional<FamilyRanking> findByAlbumIdAndPeriodAndStart(UUID albumId,
                                                             RankingPeriod period,
                                                             LocalDate start);

    Optional<FamilyRanking> findLatestByAlbumIdAndPeriod(UUID albumId, RankingPeriod period);

    // 멤버별 누적 별 수 조회 (모든 기간 합산)
    Map<String, Integer> findTotalStarsByAlbumId(UUID albumId);
}
