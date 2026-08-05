package com.memeboo2.haemi.m0.domain.model.access;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 접근 모드 추천 (F0-03). 진단·재평가로 제안(PROPOSED)되며, 가족이 명시적으로 적용(APPLIED)할 때만 모드가 바뀐다.
 * 대행 실행은 entryPath=CAREGIVER + operatorId로 기록된다.
 */
@Entity
@Table(name = "access_mode_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessModeRecommendation {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_mode", nullable = false, length = 10)
    private ElderAccessMode recommendedMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private RecommendationSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private RecommendationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_path", length = 12)
    private EntryPath entryPath;

    @Column(name = "operator_id", columnDefinition = "uuid")
    private UUID operatorId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    public static AccessModeRecommendation propose(UUID elderId, ElderAccessMode mode,
                                                   RecommendationSource source) {
        AccessModeRecommendation r = new AccessModeRecommendation();
        r.id = UUID.randomUUID();
        r.elderId = elderId;
        r.recommendedMode = mode;
        r.source = source;
        r.status = RecommendationStatus.PROPOSED;
        r.createdAt = LocalDateTime.now();
        return r;
    }

    public void apply(EntryPath entryPath, UUID operatorId, LocalDateTime now) {
        if (status != RecommendationStatus.PROPOSED) {
            throw new M0ValidationException("제안 상태의 추천만 적용할 수 있어요.");
        }
        if (entryPath == EntryPath.CAREGIVER && operatorId == null) {
            throw new M0ValidationException("대행 실행에는 operatorId가 필요해요.");
        }
        this.status = RecommendationStatus.APPLIED;
        this.entryPath = entryPath;
        this.operatorId = operatorId;
        this.appliedAt = now;
    }

    public void dismiss() {
        if (status != RecommendationStatus.PROPOSED) {
            throw new M0ValidationException("제안 상태의 추천만 기각할 수 있어요.");
        }
        this.status = RecommendationStatus.DISMISSED;
    }
}
