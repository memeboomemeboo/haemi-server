package com.memeboo2.haemi.auth.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstitutionAdminPropertiesTest {

    @Test
    @DisplayName("설정하지 않으면 아무도 기관 관리자로 가입할 수 없다")
    void defaultsToNobody() {
        assertThat(new InstitutionAdminProperties(null).normalizedAllowedEmails()).isEmpty();
        assertThat(new InstitutionAdminProperties(List.of()).normalizedAllowedEmails()).isEmpty();
    }

    @Test
    @DisplayName("빈 값만 들어와도 허용 목록으로 취급하지 않는다")
    void blankEntriesAreNotAllowlisted() {
        // 환경변수를 비워두면 [""] 로 바인딩될 수 있다. 그게 "누구나 허용"이 되면 안 된다.
        InstitutionAdminProperties properties = new InstitutionAdminProperties(Arrays.asList("", "   "));

        assertThat(properties.normalizedAllowedEmails()).isEmpty();
    }

    @Test
    @DisplayName("가입 시 이메일을 소문자로 정규화하므로 비교 기준도 맞춘다")
    void normalizesForComparison() {
        InstitutionAdminProperties properties =
                new InstitutionAdminProperties(List.of("  Admin@Haemi.KR  ", "ops@haemi.kr"));

        assertThat(properties.normalizedAllowedEmails())
                .containsExactlyInAnyOrder("admin@haemi.kr", "ops@haemi.kr");
    }
}
