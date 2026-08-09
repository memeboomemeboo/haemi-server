package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "life_stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LifeStory {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LifeStoryCategory category;

    @Column(name = "story_value", nullable = false, length = 500)
    private String value;

    @Column(nullable = false)
    private int weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LifeStorySource source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static LifeStory create(UUID elderId, LifeStoryCategory category, String value,
                                   Integer weight, LifeStorySource source) {
        if (category == null || value == null || value.isBlank() || value.trim().length() > 500) {
            throw new M0ValidationException("생애 정보 항목을 올바르게 입력해주세요.");
        }
        LifeStory story = new LifeStory();
        story.id = UUID.randomUUID();
        story.elderId = elderId;
        story.category = category;
        story.value = value.trim();
        story.weight = weight == null ? 1 : weight;
        story.source = source == null ? LifeStorySource.FAMILY : source;
        story.createdAt = LocalDateTime.now();
        return story;
    }
}
