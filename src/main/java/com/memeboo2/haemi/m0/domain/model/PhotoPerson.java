package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사진 테이블과 직접 FK를 두지 않아 M1 저장소를 교체해도 인물 안전 정책을 유지한다.
 */
@Entity
@Table(name = "photo_persons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoPerson {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "photo_id", nullable = false, columnDefinition = "uuid")
    private UUID photoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "confirmed_by_member_id", columnDefinition = "uuid")
    private UUID confirmedByMemberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PhotoPerson create(UUID photoId, Person person, double confidence, UUID confirmedByMemberId) {
        PhotoPerson tag = new PhotoPerson();
        tag.id = UUID.randomUUID();
        tag.photoId = photoId;
        tag.person = person;
        tag.apply(confidence, confirmedByMemberId);
        tag.createdAt = LocalDateTime.now();
        tag.updatedAt = tag.createdAt;
        return tag;
    }

    public void update(double confidence, UUID confirmedByMemberId) {
        apply(confidence, confirmedByMemberId);
        updatedAt = LocalDateTime.now();
    }

    public boolean canUsePersonName() {
        return confirmedByMemberId != null || confidence.compareTo(BigDecimal.valueOf(0.70)) >= 0;
    }

    private void apply(double confidence, UUID confirmedByMemberId) {
        if (confidence < 0 || confidence > 1) {
            throw new M0ValidationException("인물 태그 신뢰도는 0~1 사이여야 해요.");
        }
        this.confidence = BigDecimal.valueOf(confirmedByMemberId == null ? confidence : 1.0)
                .setScale(2);
        this.confirmedByMemberId = confirmedByMemberId;
    }
}
