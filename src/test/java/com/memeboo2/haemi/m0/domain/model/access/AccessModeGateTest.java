package com.memeboo2.haemi.m0.domain.model.access;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessModeGateTest {

    @Test
    @DisplayName("Mode A(자립)는 전체 기능을 허용한다")
    void modeA_allowsAll() {
        for (ModeFeature feature : ModeFeature.values()) {
            assertThat(AccessModeGate.isEnabled(ElderAccessMode.A, feature)).isTrue();
        }
    }

    @Test
    @DisplayName("Mode B(보조)는 안전 하위집합만 허용한다")
    void modeB_allowsSubset() {
        assertThat(AccessModeGate.isEnabled(ElderAccessMode.B, ModeFeature.RECEIVE_NOTIFICATION)).isTrue();
        assertThat(AccessModeGate.isEnabled(ElderAccessMode.B, ModeFeature.FEED_BROWSE)).isTrue();
        assertThat(AccessModeGate.isEnabled(ElderAccessMode.B, ModeFeature.SELF_SESSION_START)).isFalse();
        assertThat(AccessModeGate.isEnabled(ElderAccessMode.B, ModeFeature.DIRECT_REPLY)).isFalse();
        assertThat(AccessModeGate.isEnabled(ElderAccessMode.B, ModeFeature.ALARM_SELF_MANAGE)).isFalse();
    }

    @Test
    @DisplayName("UNSET(미설정)은 모든 기능을 차단한다")
    void unset_blocksAll() {
        assertThat(AccessModeGate.isEnabled(ElderAccessMode.UNSET, ModeFeature.RECEIVE_NOTIFICATION)).isFalse();
    }
}
