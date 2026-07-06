package com.memeboo2.haemi.m2.domain.model.ranking;

import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FamilyRankingTest {

    @Test
    @DisplayName("총 인기도 점수가 높은 구성원부터 순위를 부여한다")
    void compute_ordersByPopularityScore() {
        UUID albumId = UUID.randomUUID();
        MemoryPost high = post(albumId, "high", "고득점");
        high.submitElderReply(ReplyType.SHORT_TEXT, "답변");
        MemoryPost low = post(albumId, "low", "저득점");
        low.toggleLike("liker");

        FamilyRanking ranking = FamilyRanking.compute(
                albumId, RankingPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7),
                List.of(low, high), Map.of());

        assertThat(ranking.getEntries())
                .extracting(entry -> entry.getMemberId())
                .containsExactly("high", "low");
        assertThat(ranking.getEntries().getFirst().getAwardedStars()).isEqualTo(1);
    }

    @Test
    @DisplayName("월간 1위에게 별 3개를 부여하고 누적 뱃지를 계산한다")
    void compute_monthlyAwardsThreeStars() {
        UUID albumId = UUID.randomUUID();
        MemoryPost post = post(albumId, "member-1", "홍길동");
        post.toggleLike("liker");

        FamilyRanking ranking = FamilyRanking.compute(
                albumId, RankingPeriod.MONTHLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                List.of(post), Map.of("member-1", 2));

        assertThat(ranking.getEntries().getFirst().getAwardedStars()).isEqualTo(3);
        assertThat(ranking.getStarSummaries().getFirst().getTotalStars()).isEqualTo(5);
        assertThat(ranking.getEntries().getFirst().getBadgeGrade()).isEqualTo(BadgeGrade.MEMORY_KING);
    }

    @Test
    @DisplayName("점수가 없는 1위에게는 별을 부여하지 않는다")
    void compute_doesNotAwardStarsWithoutScore() {
        UUID albumId = UUID.randomUUID();

        FamilyRanking ranking = FamilyRanking.compute(
                albumId, RankingPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 7),
                List.of(post(albumId, "member-1", "홍길동")), Map.of());

        assertThat(ranking.getEntries().getFirst().getAwardedStars()).isZero();
    }

    @Test
    @DisplayName("누적 별 수에 따라 뱃지 등급을 계산한다")
    void badgeGrade_mapsStarBoundaries() {
        assertThat(BadgeGrade.fromStars(0)).isEqualTo(BadgeGrade.NONE);
        assertThat(BadgeGrade.fromStars(1)).isEqualTo(BadgeGrade.LOVE_SPROUT);
        assertThat(BadgeGrade.fromStars(5)).isEqualTo(BadgeGrade.MEMORY_KING);
        assertThat(BadgeGrade.fromStars(10)).isEqualTo(BadgeGrade.MEMORY_KING_PLUS);
    }

    private MemoryPost post(UUID albumId, String memberId, String name) {
        return MemoryPost.createDraft(
                albumId, AuthorInfo.of(memberId, name, "가족"), "내용", null, null);
    }
}
