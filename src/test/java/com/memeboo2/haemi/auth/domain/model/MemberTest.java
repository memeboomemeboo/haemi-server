package com.memeboo2.haemi.auth.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    @DisplayName("회원 생성 시 이메일과 이름을 정규화하고 활성 상태가 된다")
    void create_normalizesFields() {
        Member member = Member.create(
                "  USER@Example.COM  ", "encoded", "  홍길동  ", MemberRole.FAMILY);

        assertThat(member.getId()).isNotNull();
        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.isActive()).isTrue();
        assertThat(member.getCreatedAt()).isEqualTo(member.getUpdatedAt());
    }

    @Test
    @DisplayName("잘못된 이메일은 회원 생성이 거부된다")
    void create_rejectsInvalidEmail() {
        assertThatThrownBy(() -> Member.create("invalid", "encoded", "홍길동", MemberRole.FAMILY))
                .isInstanceOf(InvalidEmailException.class);
        assertThatThrownBy(() -> Member.create(" ", "encoded", "홍길동", MemberRole.FAMILY))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    @DisplayName("빈 이름과 50자를 초과한 이름은 거부된다")
    void create_rejectsInvalidName() {
        assertThatThrownBy(() -> Member.create("user@example.com", "encoded", " ", MemberRole.FAMILY))
                .isInstanceOf(InvalidNameException.class);
        assertThatThrownBy(() -> Member.create(
                "user@example.com", "encoded", "가".repeat(51), MemberRole.FAMILY))
                .isInstanceOf(InvalidNameException.class);
    }

    @Test
    @DisplayName("회원 탈퇴 시 Refresh Token을 제거하고 재탈퇴를 막는다")
    void withdraw_clearsRefreshTokenAndCannotRepeat() {
        Member member = member(MemberRole.FAMILY);
        member.updateRefreshTokenHash("hash");

        member.withdraw();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getRefreshTokenHash()).isNull();
        assertThatThrownBy(member::withdraw)
                .isInstanceOf(AlreadyWithdrawnException.class);
    }

    @Test
    @DisplayName("회원 정지 시 Refresh Token이 제거되고 다시 활성화할 수 있다")
    void suspendAndActivate_changesStatus() {
        Member member = member(MemberRole.FAMILY);
        member.updateRefreshTokenHash("hash");

        member.suspend();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(member.getRefreshTokenHash()).isNull();

        member.activate();
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("기관 관리자는 TOTP 설정 여부와 무관하게 TOTP가 필요한 역할이다")
    void institutionAdmin_requiresTotp() {
        Member admin = member(MemberRole.INSTITUTION_ADMIN);

        assertThat(admin.requiresTotp()).isTrue();
        assertThat(admin.isTotpEnabled()).isFalse();

        admin.enableTotp("secret");
        assertThat(admin.isTotpEnabled()).isTrue();

        admin.disableTotp();
        assertThat(admin.isTotpEnabled()).isFalse();
    }

    private Member member(MemberRole role) {
        return Member.create("user@example.com", "encoded", "홍길동", role);
    }
}
