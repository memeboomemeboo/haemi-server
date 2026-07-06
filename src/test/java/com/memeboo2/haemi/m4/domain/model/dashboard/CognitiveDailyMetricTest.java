package com.memeboo2.haemi.m4.domain.model.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CognitiveDailyMetricTest {

    @Test
    @DisplayName("일일 지표 생성 시 음수와 정확도 범위를 보정한다")
    void create_clampsInvalidValues() {
        CognitiveDailyMetric metric = CognitiveDailyMetric.create(
                "elder", UUID.randomUUID(), "institution", LocalDate.now(),
                -1, 1.5, -10, -2, -3, "가족사진");

        assertThat(metric.getTrainingSessionCount()).isZero();
        assertThat(metric.getTrainingAccuracyRate()).isEqualTo(1.0);
        assertThat(metric.getAverageResponseSeconds()).isZero();
        assertThat(metric.getReminiscenceReactionCount()).isZero();
        assertThat(metric.getMemoryPostCount()).isZero();
        assertThat(metric.participated()).isFalse();
    }

    @Test
    @DisplayName("훈련 결과 병합 시 누적 평균을 계산한다")
    void mergeTrainingResult_calculatesWeightedAverage() {
        UUID albumId = UUID.randomUUID();
        CognitiveDailyMetric metric = CognitiveDailyMetric.create(
                "elder", albumId, "institution", LocalDate.now(),
                1, 0.8, 20, 0, 0, null);

        metric.mergeTrainingResult(albumId, 0.4, 40);

        assertThat(metric.getTrainingSessionCount()).isEqualTo(2);
        assertThat(metric.getTrainingAccuracyRate()).isCloseTo(0.6, within(1.0e-9));
        assertThat(metric.getAverageResponseSeconds()).isEqualTo(30.0);
        assertThat(metric.participated()).isTrue();
    }

    @Test
    @DisplayName("스냅샷 갱신 시 모든 집계값을 교체하고 범위를 보정한다")
    void updateSnapshot_replacesValues() {
        CognitiveDailyMetric metric = CognitiveDailyMetric.create(
                "elder", UUID.randomUUID(), "old", LocalDate.now(),
                0, 0, 0, 0, 0, null);

        metric.updateSnapshot("new", 2, -0.5, 15, 3, 4, "풍경");

        assertThat(metric.getInstitutionId()).isEqualTo("new");
        assertThat(metric.getTrainingSessionCount()).isEqualTo(2);
        assertThat(metric.getTrainingAccuracyRate()).isZero();
        assertThat(metric.getAverageResponseSeconds()).isEqualTo(15);
        assertThat(metric.getReminiscenceReactionCount()).isEqualTo(3);
        assertThat(metric.getMemoryPostCount()).isEqualTo(4);
        assertThat(metric.getMostReactedPhotoType()).isEqualTo("풍경");
    }
}
