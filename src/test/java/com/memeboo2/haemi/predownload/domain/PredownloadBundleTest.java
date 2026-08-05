package com.memeboo2.haemi.predownload.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PredownloadBundleTest {

    @Test
    @DisplayName("자산 총합은 카드·사진·힌트 수의 합이다")
    void totalAssets_sumsAllKinds() {
        PredownloadBundle bundle = PredownloadBundle.of("elder-1", LocalDate.of(2026, 8, 5),
                List.of("c1", "c2"), List.of("p1"), List.of("h1", "h2", "h3"));

        assertThat(bundle.totalAssets()).isEqualTo(6);
        assertThat(bundle.isEmpty()).isFalse();
        assertThat(bundle.assembledAt()).isNotNull();
    }

    @Test
    @DisplayName("모든 자산이 비면 빈 번들이다")
    void isEmpty_whenNoAssets() {
        PredownloadBundle bundle = PredownloadBundle.of("elder-1", LocalDate.of(2026, 8, 5),
                List.of(), List.of(), List.of());

        assertThat(bundle.isEmpty()).isTrue();
        assertThat(bundle.totalAssets()).isZero();
    }
}
