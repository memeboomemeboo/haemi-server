package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.ranking.FamilyRanking;
import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;
import com.memeboo2.haemi.m2.domain.repository.FamilyRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class FamilyRankingRepositoryAdapter implements FamilyRankingRepository {

    private final JpaFamilyRankingRepository jpa;

    @Override
    public FamilyRanking save(FamilyRanking ranking) {
        return jpa.save(ranking);
    }

    @Override
    public Optional<FamilyRanking> findByAlbumIdAndPeriodAndStart(
            UUID albumId, RankingPeriod period, LocalDate start) {
        return jpa.findByAlbumIdAndPeriodAndPeriodStart(albumId, period, start);
    }

    @Override
    public Optional<FamilyRanking> findLatestByAlbumIdAndPeriod(UUID albumId, RankingPeriod period) {
        return jpa.findLatest(albumId, period);
    }

    @Override
    public Map<String, Integer> findTotalStarsByAlbumId(UUID albumId) {
        // 모든 기간 랭킹에서 구성원별 awardedStars 합산
        Map<String, Integer> starMap = new HashMap<>();
        jpa.findAllByAlbumId(albumId).forEach(ranking ->
                ranking.getEntries().forEach(entry ->
                        starMap.merge(entry.getMemberId(), entry.getAwardedStars(), Integer::sum)));
        return starMap;
    }
}
