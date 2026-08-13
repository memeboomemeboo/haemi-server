package com.memeboo2.haemi.m3.domain.model.hint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HintBankResolverTest {

    @Test
    @DisplayName("L1: 사진 특정 적립 힌트가 있으면 우선 제공한다")
    void resolve_prefersL1() {
        AccruedHint l1 = AccruedHint.accrue("elder", UUID.randomUUID(), "손녀",
                AccrualSource.MEMO, "m1", "지민", "그때 바닷가 기억나세요?");
        ResolvedHint resolved = HintBankResolver.resolve(Optional.of(l1));

        assertThat(resolved.tier()).isEqualTo(HintTier.L1);
        assertThat(resolved.text()).isEqualTo("그때 바닷가 기억나세요?");
        assertThat(resolved.responderName()).isEqualTo("지민");
    }

    @Test
    @DisplayName("L3: 적립 힌트가 없으면 시스템 기본 문구를 제공한다")
    void resolve_fallsBackToL3() {
        ResolvedHint resolved = HintBankResolver.resolve(Optional.empty());

        assertThat(resolved.tier()).isEqualTo(HintTier.L3);
        assertThat(resolved.text()).isEqualTo(HintBankResolver.DEFAULT_HINT_TEXT);
        assertThat(resolved.responderName()).isEqualTo(HintBankResolver.SYSTEM_RESPONDER);
    }

    @Test
    @DisplayName("사전 적립 힌트는 반드시 특정 사진에 연결해야 한다")
    void accrue_requiresPhoto() {
        assertThatThrownBy(() -> AccruedHint.accrue("elder", null, "손녀",
                AccrualSource.MEMO, "m1", "지민", "힌트"))
                .hasMessageContaining("연결할 사진");
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

    @Test
    @DisplayName("L1 사진 힌트는 재생 뒤 14일 동안 다시 제공하지 않는다")
    void l1Hint_cannotBeReusedForFourteenDays() {
        AccruedHint hint = AccruedHint.accrue("elder", UUID.randomUUID(), "손녀",
                AccrualSource.MEMO, "m1", "지민", "힌트");
        LocalDateTime servedAt = LocalDateTime.of(2026, 8, 11, 9, 0);
        hint.markServed(servedAt);

        assertThat(hint.isReusableAt(servedAt.plusDays(13))).isFalse();
        assertThat(hint.isReusableAt(servedAt.plusDays(14))).isTrue();
    }
}
