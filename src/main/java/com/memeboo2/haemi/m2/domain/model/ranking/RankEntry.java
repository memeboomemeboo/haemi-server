package com.memeboo2.haemi.m2.domain.model.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankEntry {

    @Column(name = "entry_member_id", nullable = false)
    private String memberId;

    @Column(name = "entry_member_name", nullable = false)
    private String memberName;

    @Column(name = "entry_rank", nullable = false)
    private int rank;

    @Column(name = "entry_score", nullable = false)
    private int score;

    @Column(name = "entry_post_count", nullable = false)
    private int postCount;

    @Column(name = "entry_reply_count", nullable = false)
    private int replyCount;

    // 이번 기간에 지급된 별
    @Column(name = "awarded_stars", nullable = false)
    private int awardedStars;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_grade")
    private BadgeGrade badgeGrade;

    private RankEntry(String memberId, String memberName, int rank,
                      int score, int postCount, int replyCount,
                      int awardedStars, BadgeGrade badgeGrade) {
        this.memberId     = memberId;
        this.memberName   = memberName;
        this.rank         = rank;
        this.score        = score;
        this.postCount    = postCount;
        this.replyCount   = replyCount;
        this.awardedStars = awardedStars;
        this.badgeGrade   = badgeGrade;
    }

    public static RankEntry of(String memberId, String memberName, int rank,
                                int score, int postCount, int replyCount,
                                int awardedStars, BadgeGrade badgeGrade) {
        return new RankEntry(memberId, memberName, rank, score,
                postCount, replyCount, awardedStars, badgeGrade);
    }
}
