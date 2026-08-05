package com.memeboo2.haemi.m3.domain.model.hint;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * F3-03 사전 적립형 손주 한마디.
 * 가족이 미리 적립해 두고, 회상 세션 중 대기 없이 즉시 제공된다.
 * photoId가 있으면 특정 사진(L1), 없으면 어르신 일반(L2) 힌트다.
 */
@Entity
@Table(name = "accrued_hints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccruedHint {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "photo_id", columnDefinition = "uuid")
    private UUID photoId;

    @Column(name = "person_name")
    private String personName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private AccrualSource source;

    @Column(name = "author_member_id", nullable = false)
    private String authorMemberId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "hint_text", nullable = false, length = 500)
    private String text;

    // S1(EX-F303-07/08) 억제 훅: 숨김 인물·작고한 가족 힌트는 비활성 처리해 제공에서 제외
    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private AccruedHint(String elderId, UUID photoId, String personName, AccrualSource source,
                        String authorMemberId, String authorName, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("힌트 내용을 입력해주세요.");
        }
        if (source == null) {
            throw new IllegalArgumentException("적립 경로는 필수입니다.");
        }
        this.id = UUID.randomUUID();
        this.elderId = elderId;
        this.photoId = photoId;
        this.personName = personName;
        this.source = source;
        this.authorMemberId = authorMemberId;
        this.authorName = authorName;
        this.text = text.trim();
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public static AccruedHint accrue(String elderId, UUID photoId, String personName, AccrualSource source,
                                     String authorMemberId, String authorName, String text) {
        return new AccruedHint(elderId, photoId, personName, source, authorMemberId, authorName, text);
    }

    public HintTier tier() {
        return photoId != null ? HintTier.L1 : HintTier.L2;
    }

    public void suppress() {
        this.active = false;
    }
}
