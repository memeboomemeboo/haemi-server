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
public class MemberStarSummary {

    @Column(name = "star_member_id", nullable = false)
    private String memberId;

    @Column(name = "total_stars", nullable = false)
    private int totalStars;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_badge")
    private BadgeGrade currentBadge;

    private MemberStarSummary(String memberId, int totalStars, BadgeGrade currentBadge) {
        this.memberId     = memberId;
        this.totalStars   = totalStars;
        this.currentBadge = currentBadge;
    }

    public static MemberStarSummary of(String memberId, int totalStars) {
        return new MemberStarSummary(memberId, totalStars, BadgeGrade.fromStars(totalStars));
    }

    public MemberStarSummary addStars(int stars) {
        int newTotal = this.totalStars + stars;
        return new MemberStarSummary(this.memberId, newTotal, BadgeGrade.fromStars(newTotal));
    }

    public boolean isBadgeUpgraded(MemberStarSummary previous) {
        return this.currentBadge != previous.currentBadge;
    }
}
