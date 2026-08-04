package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensitive_topics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SensitiveTopic {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(length = 300)
    private String reason;

    @Column(name = "created_by_member_id", nullable = false, columnDefinition = "uuid")
    private UUID createdByMemberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SensitiveTopic create(UUID elderId, String keyword, String reason, UUID createdByMemberId) {
        if (keyword == null || keyword.trim().length() < 2 || keyword.trim().length() > 100) {
            throw new M0ValidationException("피해야 할 주제는 2~100자로 입력해주세요.");
        }
        if (reason != null && reason.trim().length() > 300) {
            throw new M0ValidationException("피해야 할 주제 설명은 300자를 넘을 수 없어요.");
        }
        SensitiveTopic topic = new SensitiveTopic();
        topic.id = UUID.randomUUID();
        topic.elderId = elderId;
        topic.keyword = keyword.trim();
        topic.reason = reason == null || reason.isBlank() ? null : reason.trim();
        topic.createdByMemberId = createdByMemberId;
        topic.createdAt = LocalDateTime.now();
        return topic;
    }
}
