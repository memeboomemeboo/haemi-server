package com.memeboo2.haemi.m1.domain.model.album;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Embeddable
@Getter
@EqualsAndHashCode(of = "memberId")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumMember {

    private static final Duration INVITE_EXPIRY = Duration.ofHours(24);

    @Column(name = "member_id")
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    private AlbumMember(String memberId, MembershipStatus status, LocalDateTime invitedAt) {
        this.memberId = memberId;
        this.status = status;
        this.invitedAt = invitedAt;
    }

    static AlbumMember accepted(String memberId) {
        return new AlbumMember(memberId, MembershipStatus.ACCEPTED, LocalDateTime.now());
    }

    static AlbumMember pending(String memberId) {
        return pending(memberId, LocalDateTime.now());
    }

    // 초대 시각을 직접 지정할 수 있도록 열어둔 오버로드 (만료 검증 테스트용)
    static AlbumMember pending(String memberId, LocalDateTime invitedAt) {
        return new AlbumMember(memberId, MembershipStatus.PENDING, invitedAt);
    }

    boolean isExpired() {
        return status == MembershipStatus.PENDING
                && invitedAt.plus(INVITE_EXPIRY).isBefore(LocalDateTime.now());
    }

    void accept() {
        this.status = MembershipStatus.ACCEPTED;
    }

    // 이미 PENDING 상태인 초대를 재초대하면 만료 시각을 다시 24시간으로 늘려준다
    void refreshInviteIfPending() {
        if (status == MembershipStatus.PENDING) {
            this.invitedAt = LocalDateTime.now();
        }
    }
}
