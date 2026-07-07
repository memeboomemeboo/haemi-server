package com.memeboo2.haemi.m1.domain.model.album;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumMemberTest {

    @Test
    @DisplayName("초대 24시간 이내에는 만료되지 않는다")
    void isExpired_falseWithinWindow() {
        AlbumMember member = AlbumMember.pending("member-1", LocalDateTime.now().minusHours(23));

        assertThat(member.isExpired()).isFalse();
    }

    @Test
    @DisplayName("초대 24시간이 지나면 만료된다")
    void isExpired_trueAfterWindow() {
        AlbumMember member = AlbumMember.pending("member-1", LocalDateTime.now().minusHours(25));

        assertThat(member.isExpired()).isTrue();
    }

    @Test
    @DisplayName("수락된 구성원은 시간이 지나도 만료되지 않는다")
    void isExpired_falseOnceAccepted() {
        AlbumMember member = AlbumMember.pending("member-1", LocalDateTime.now().minusHours(25));
        member.accept();

        assertThat(member.isExpired()).isFalse();
        assertThat(member.getStatus()).isEqualTo(MembershipStatus.ACCEPTED);
    }
}
