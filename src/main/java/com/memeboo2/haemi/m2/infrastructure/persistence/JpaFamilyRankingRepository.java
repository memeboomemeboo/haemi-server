package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.ranking.FamilyRanking;
import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaFamilyRankingRepository extends JpaRepository<FamilyRanking, UUID> {

    Optional<FamilyRanking> findByAlbumIdAndPeriodAndPeriodStart(
            UUID albumId, RankingPeriod period, LocalDate periodStart);

    @Query("""
            SELECT r FROM FamilyRanking r
            WHERE r.albumId = :albumId AND r.period = :period
            ORDER BY r.periodStart DESC
            LIMIT 1
            """)
    Optional<FamilyRanking> findLatest(UUID albumId, RankingPeriod period);

    List<FamilyRanking> findAllByAlbumId(UUID albumId);
}
