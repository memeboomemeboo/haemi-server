package com.memeboo2.haemi.m3.domain.model.hint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HintBankResolverTest {

    @Test
    @DisplayName("L1: 사진 특정 적립 힌트가 있으면 우선 제공한다")
    void resolve_prefersL1() {
        AccruedHint l1 = AccruedHint.accrue("elder", UUID.randomUUID(), "손녀",
                AccrualSource.MEMO, "m1", "지민", "그때 바닷가 기억나세요?");
        AccruedHint l2 = AccruedHint.accrue("elder", null, null,
                AccrualSource.ONBOARDING, "m2", "지호", "천천히 떠올려보세요.");

        ResolvedHint resolved = HintBankResolver.resolve(Optional.of(l1), Optional.of(l2));

        assertThat(resolved.tier()).isEqualTo(HintTier.L1);
        assertThat(resolved.text()).isEqualTo("그때 바닷가 기억나세요?");
        assertThat(resolved.responderName()).isEqualTo("지민");
    }

    @Test
    @DisplayName("L2: L1이 없으면 어르신 일반 적립 힌트로 폴백한다")
    void resolve_fallsBackToL2() {
        AccruedHint l2 = AccruedHint.accrue("elder", null, null,
                AccrualSource.WEEKLY_REMINDER, "m2", "지호", "천천히 떠올려보세요.");

        ResolvedHint resolved = HintBankResolver.resolve(Optional.empty(), Optional.of(l2));

        assertThat(resolved.tier()).isEqualTo(HintTier.L2);
        assertThat(resolved.responderName()).isEqualTo("지호");
    }

    @Test
    @DisplayName("L3: 적립 힌트가 없으면 시스템 기본 문구를 제공한다")
    void resolve_fallsBackToL3() {
        ResolvedHint resolved = HintBankResolver.resolve(Optional.empty(), Optional.empty());

        assertThat(resolved.tier()).isEqualTo(HintTier.L3);
        assertThat(resolved.text()).isEqualTo(HintBankResolver.DEFAULT_HINT_TEXT);
        assertThat(resolved.responderName()).isEqualTo(HintBankResolver.SYSTEM_RESPONDER);
    }

    @Test
    @DisplayName("suppress된 힌트는 tier 판정과 무관하게 저장소 조회에서 제외되는 전제 — active 플래그를 끈다")
    void suppress_deactivates() {
        AccruedHint hint = AccruedHint.accrue("elder", UUID.randomUUID(), "손녀",
                AccrualSource.REACTION, "m1", "지민", "힌트");
        assertThat(hint.isActive()).isTrue();

        hint.suppress();

        assertThat(hint.isActive()).isFalse();
    }
}
