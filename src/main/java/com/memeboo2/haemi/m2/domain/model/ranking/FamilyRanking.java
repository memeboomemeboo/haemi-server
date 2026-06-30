package com.memeboo2.haemi.m2.domain.model.ranking;

import com.memeboo2.haemi.m2.domain.event.BadgeAwardedEvent;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "family_rankings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"album_id", "period", "period_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyRanking extends AbstractAggregateRoot<FamilyRanking> {

    // 주간 1위 별 1개, 월간 1위 별 3개
    private static final int WEEKLY_STARS  = 1;
    private static final int MONTHLY_STARS = 3;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private RankingPeriod period;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ranking_entries",
            joinColumns = @JoinColumn(name = "ranking_id"))
    @OrderBy("entry_rank ASC")
    private List<RankEntry> entries = new ArrayList<>();

    // 앨범 내 구성원별 누적 별 집계
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_star_summaries",
            joinColumns = @JoinColumn(name = "ranking_id"))
    private List<MemberStarSummary> starSummaries = new ArrayList<>();

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    public static FamilyRanking compute(UUID albumId, RankingPeriod period,
                                         LocalDate start, LocalDate end,
                                         List<MemoryPost> posts,
                                         Map<String, Integer> prevStarMap) {
        FamilyRanking ranking = new FamilyRanking();
        ranking.id          = UUID.randomUUID();
        ranking.albumId     = albumId;
        ranking.period      = period;
        ranking.periodStart = start;
        ranking.periodEnd   = end;
        ranking.computedAt  = LocalDateTime.now();

        // 멤버별 점수·게시 수·답변 수 집계
        Map<String, MemberStat> stats = new LinkedHashMap<>();
        for (MemoryPost post : posts) {
            String mid  = post.getAuthorInfo().getMemberId();
            String name = post.getAuthorInfo().getMemberName();
            stats.computeIfAbsent(mid, k -> new MemberStat(mid, name));
            stats.get(mid).addPost(post.getPopularityScore(), post.hasElderReply());
        }

        // 동점 시: 어르신 답변 수 우선 → 게시 수 우선 → 게시 시간(index 작을수록 빠름)
        List<MemberStat> sorted = stats.values().stream()
                .sorted(Comparator.comparingInt(MemberStat::score).reversed()
                        .thenComparingInt(MemberStat::replyCount).reversed()
                        .thenComparingInt(MemberStat::postCount).reversed())
                .toList();

        int starsForFirst = (period == RankingPeriod.WEEKLY) ? WEEKLY_STARS : MONTHLY_STARS;
        List<MemberStarSummary> summaries = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            MemberStat stat  = sorted.get(i);
            int rank         = i + 1;
            int awarded      = (rank == 1 && stat.score() > 0) ? starsForFirst : 0;
            int prevStars    = prevStarMap.getOrDefault(stat.memberId(), 0);
            MemberStarSummary prev    = MemberStarSummary.of(stat.memberId(), prevStars);
            MemberStarSummary updated = prev.addStars(awarded);

            ranking.entries.add(RankEntry.of(
                    stat.memberId(), stat.memberName(), rank,
                    stat.score(), stat.postCount(), stat.replyCount(),
                    awarded, updated.getCurrentBadge()
            ));
            summaries.add(updated);

            // 뱃지 업그레이드 시 이벤트 발행
            if (awarded > 0 && updated.isBadgeUpgraded(prev)) {
                ranking.registerEvent(new BadgeAwardedEvent(
                        albumId, stat.memberId(), stat.memberName(),
                        updated.getCurrentBadge(), LocalDateTime.now()));
            }
        }
        ranking.starSummaries.addAll(summaries);
        return ranking;
    }

    public List<RankEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    // 내부 집계용 레코드
    private static class MemberStat {
        private final String memberId;
        private final String memberName;
        private int totalScore;
        private int postCount;
        private int replyCount;

        MemberStat(String memberId, String memberName) {
            this.memberId   = memberId;
            this.memberName = memberName;
        }

        void addPost(int score, boolean hasReply) {
            this.totalScore += score;
            this.postCount++;
            if (hasReply) this.replyCount++;
        }

        String memberId()   { return memberId; }
        String memberName() { return memberName; }
        int score()         { return totalScore; }
        int postCount()     { return postCount; }
        int replyCount()    { return replyCount; }
    }
}
