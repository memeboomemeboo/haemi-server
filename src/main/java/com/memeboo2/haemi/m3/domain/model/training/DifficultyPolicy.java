package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "difficulty_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DifficultyPolicy {

    @Id
    @Column(name = "difficulty_level")
    private int level;

    @Column(name = "max_average_response_seconds", nullable = false)
    private double maxAverageResponseSeconds;

    @Column(name = "increase_accuracy_threshold", nullable = false)
    private double increaseAccuracyThreshold;

    @Column(name = "decrease_accuracy_threshold", nullable = false)
    private double decreaseAccuracyThreshold;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "difficulty_policy_question_types",
            joinColumns = @JoinColumn(name = "difficulty_level")
    )
    @Column(name = "question_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<QuestionType> questionTypes = new LinkedHashSet<>();

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", nullable = false, length = 100)
    private String reviewedBy;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DifficultyPolicy defaultFor(int level) {
        int normalizedLevel = clampLevel(level);
        double maxResponseSeconds = switch (normalizedLevel) {
            case 1 -> 20.0;
            case 2 -> 25.0;
            case 3 -> 30.0;
            case 4 -> 40.0;
            default -> 50.0;
        };
        LocalDateTime now = LocalDateTime.now();
        DifficultyPolicy policy = new DifficultyPolicy();
        policy.level = normalizedLevel;
        policy.maxAverageResponseSeconds = maxResponseSeconds;
        policy.increaseAccuracyThreshold = 0.8;
        policy.decreaseAccuracyThreshold = 0.4;
        policy.questionTypes.addAll(Set.of(QuestionType.values()));
        policy.reviewedAt = now;
        policy.reviewedBy = "system-default";
        policy.nextReviewDate = now.toLocalDate().plusMonths(3);
        policy.updatedAt = now;
        return policy;
    }

    public void update(double maxAverageResponseSeconds,
                       double increaseAccuracyThreshold,
                       double decreaseAccuracyThreshold,
                       Set<QuestionType> questionTypes,
                       String reviewedBy,
                       LocalDate reviewedDate) {
        validate(
                maxAverageResponseSeconds,
                increaseAccuracyThreshold,
                decreaseAccuracyThreshold,
                questionTypes,
                reviewedBy
        );
        LocalDate effectiveReviewDate = reviewedDate != null ? reviewedDate : LocalDate.now();
        this.maxAverageResponseSeconds = maxAverageResponseSeconds;
        this.increaseAccuracyThreshold = increaseAccuracyThreshold;
        this.decreaseAccuracyThreshold = decreaseAccuracyThreshold;
        this.questionTypes.clear();
        this.questionTypes.addAll(questionTypes);
        this.reviewedBy = reviewedBy.trim();
        this.reviewedAt = effectiveReviewDate.atStartOfDay();
        this.nextReviewDate = effectiveReviewDate.plusMonths(3);
        this.updatedAt = LocalDateTime.now();
    }

    public Set<QuestionType> getQuestionTypes() {
        return Collections.unmodifiableSet(questionTypes);
    }

    private static void validate(double maxAverageResponseSeconds,
                                 double increaseAccuracyThreshold,
                                 double decreaseAccuracyThreshold,
                                 Set<QuestionType> questionTypes,
                                 String reviewedBy) {
        if (maxAverageResponseSeconds <= 0.0) {
            throw new IllegalArgumentException("평균 반응 시간 기준은 0초보다 커야 합니다.");
        }
        if (increaseAccuracyThreshold < 0.0 || increaseAccuracyThreshold > 1.0
                || decreaseAccuracyThreshold < 0.0 || decreaseAccuracyThreshold > 1.0) {
            throw new IllegalArgumentException("정답률 기준은 0.0~1.0 범위여야 합니다.");
        }
        if (decreaseAccuracyThreshold >= increaseAccuracyThreshold) {
            throw new IllegalArgumentException("하락 정답률 기준은 상승 정답률 기준보다 낮아야 합니다.");
        }
        if (questionTypes == null || questionTypes.size() < 2) {
            throw new IllegalArgumentException("연속 유형 중복 방지를 위해 문제 유형을 두 개 이상 선택해야 합니다.");
        }
        if (reviewedBy == null || reviewedBy.isBlank()) {
            throw new IllegalArgumentException("검토자를 입력해야 합니다.");
        }
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(5, level));
    }
}
