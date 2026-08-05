package com.memeboo2.haemi.m0.domain.model.access;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessModeAssessmentTest {

    @Test
    @DisplayName("합계 6점 이상이면 자립(A), 미만이면 보조(B)를 추천한다")
    void recommend_thresholdBoundary() {
        assertThat(AccessModeAssessment.recommend(List.of(2, 2, 2, 0, 0))).isEqualTo(ElderAccessMode.A); // 6
        assertThat(AccessModeAssessment.recommend(List.of(1, 1, 1, 1, 1))).isEqualTo(ElderAccessMode.B); // 5
        assertThat(AccessModeAssessment.recommend(List.of(2, 2, 2, 2, 2))).isEqualTo(ElderAccessMode.A); // 10
        assertThat(AccessModeAssessment.recommend(List.of(0, 0, 0, 0, 0))).isEqualTo(ElderAccessMode.B); // 0
    }

    @Test
    @DisplayName("문항 수가 5가 아니거나 점수 범위를 벗어나면 거부한다")
    void recommend_validatesInput() {
        assertThatThrownBy(() -> AccessModeAssessment.recommend(List.of(2, 2, 2, 2)))
                .isInstanceOf(M0ValidationException.class);
        assertThatThrownBy(() -> AccessModeAssessment.recommend(List.of(3, 0, 0, 0, 0)))
                .isInstanceOf(M0ValidationException.class);
    }
}
