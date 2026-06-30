package com.memeboo2.haemi.m2.application.dto;

import com.memeboo2.haemi.m2.domain.model.ranking.BadgeGrade;
import com.memeboo2.haemi.m2.domain.model.ranking.FamilyRanking;
import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RankingResult(
        String rankingId,
        String albumId,
        RankingPeriod period,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<RankEntryResult> entries,
        LocalDateTime computedAt
) {
    public record RankEntryResult(
            int rank,
            String memberId,
            String memberName,
            int score,
            int postCount,
            int replyCount,
            int awardedStars,
            BadgeGrade badgeGrade,
            String badgeLabel
    ) {}

    public static RankingResult from(FamilyRanking ranking) {
        List<RankEntryResult> entries = ranking.getEntries().stream()
                .map(e -> new RankEntryResult(
                        e.getRank(), e.getMemberId(), e.getMemberName(),
                        e.getScore(), e.getPostCount(), e.getReplyCount(),
                        e.getAwardedStars(), e.getBadgeGrade(),
                        e.getBadgeGrade().getLabel()
                )).toList();
        return new RankingResult(
                ranking.getId().toString(),
                ranking.getAlbumId().toString(),
                ranking.getPeriod(),
                ranking.getPeriodStart(),
                ranking.getPeriodEnd(),
                entries,
                ranking.getComputedAt()
        );
    }
}
