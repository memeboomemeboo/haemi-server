package com.memeboo2.haemi.m0.domain.model.access;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessModeRecommendationTest {

    private AccessModeRecommendation proposed() {
        return AccessModeRecommendation.propose(UUID.randomUUID(), ElderAccessMode.B,
                RecommendationSource.INITIAL);
    }

    @Test
    @DisplayName("대행 적용은 entryPath=CAREGIVER와 operatorId를 기록한다")
    void apply_recordsProxy() {
        AccessModeRecommendation reco = proposed();
        UUID operator = UUID.randomUUID();

        reco.apply(EntryPath.CAREGIVER, operator, LocalDateTime.now());

        assertThat(reco.getStatus()).isEqualTo(RecommendationStatus.APPLIED);
        assertThat(reco.getEntryPath()).isEqualTo(EntryPath.CAREGIVER);
        assertThat(reco.getOperatorId()).isEqualTo(operator);
        assertThat(reco.getAppliedAt()).isNotNull();
    }

    @Test
    @DisplayName("대행 적용에 operatorId가 없으면 거부한다")
    void apply_caregiverRequiresOperator() {
        assertThatThrownBy(() -> proposed().apply(EntryPath.CAREGIVER, null, LocalDateTime.now()))
                .isInstanceOf(M0ValidationException.class);
    }

    @Test
    @DisplayName("이미 적용된 추천은 다시 적용·기각할 수 없다")
    void apply_onlyWhenProposed() {
        AccessModeRecommendation reco = proposed();
        reco.apply(EntryPath.SELF, null, LocalDateTime.now());

        assertThatThrownBy(() -> reco.apply(EntryPath.SELF, null, LocalDateTime.now()))
                .isInstanceOf(M0ValidationException.class);
        assertThatThrownBy(reco::dismiss).isInstanceOf(M0ValidationException.class);
    }
}
