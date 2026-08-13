package com.memeboo2.haemi.m4.presentation;

import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstitutionPortalRateLimiterTest {

    @Test
    void limitsAnInstitutionOperatorToOneHundredRequestsPerMinute() {
        InstitutionPortalRateLimiter limiter = new InstitutionPortalRateLimiter();
        UUID memberId = UUID.randomUUID();

        for (int i = 0; i < 100; i++) {
            assertThatCode(() -> limiter.check(memberId)).doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> limiter.check(memberId)).isInstanceOf(M0AccessDeniedException.class);
    }
}
