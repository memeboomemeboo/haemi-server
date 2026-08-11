package com.memeboo2.haemi.m3.domain.model.training;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DifficultyPolicyTest {

    @Test
    @DisplayName("기본 기준표는 레벨별 반응 시간과 분기 검토일을 제공한다")
    void defaultPolicy_hasLevelSpecificThresholdAndQuarterlyReview() {
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(3);

        assertThat(policy.getLevel()).isEqualTo(3);
        assertThat(policy.getMaxAverageResponseSeconds()).isEqualTo(30.0);
        assertThat(policy.getIncreaseAccuracyThreshold()).isEqualTo(0.8);
        assertThat(policy.getDecreaseAccuracyThreshold()).isEqualTo(0.4);
        assertThat(policy.getNextReviewDate())
                .isEqualTo(policy.getReviewedAt().toLocalDate().plusMonths(3));
    }

    @Test
    @DisplayName("전문가 검토 기준을 갱신하면 다음 검토일을 3개월 후로 계산한다")
    void update_setsReviewerAndNextReviewDate() {
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(2);
        LocalDate reviewedDate = LocalDate.of(2026, 7, 6);

        policy.update(
                28.0,
                0.85,
                0.35,
                EnumSet.of(QuestionType.PERSON_RECALL, QuestionType.PLACE_MATCH),
                "expert@haemi.kr",
                reviewedDate
        );

        assertThat(policy.getMaxAverageResponseSeconds()).isEqualTo(28.0);
        assertThat(policy.getReviewedBy()).isEqualTo("expert@haemi.kr");
        assertThat(policy.getReviewedAt()).isEqualTo(reviewedDate.atStartOfDay());
        assertThat(policy.getNextReviewDate()).isEqualTo(LocalDate.of(2026, 10, 6));
    }

    @Test
    @DisplayName("잘못된 정답률 범위와 한 개 이하 문제 유형은 거부한다")
    void update_rejectsInvalidPolicy() {
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(2);

        assertThatThrownBy(() -> policy.update(
                25.0,
                0.3,
                0.4,
                Set.of(QuestionType.PLACE_MATCH, QuestionType.PERSON_RECALL),
                "expert",
                LocalDate.now()
        )).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> policy.update(
                25.0,
                0.8,
                0.4,
                Set.of(QuestionType.PERSON_RECALL),
                "expert",
                LocalDate.now()
        )).isInstanceOf(DomainValidationException.class);
    }
}
