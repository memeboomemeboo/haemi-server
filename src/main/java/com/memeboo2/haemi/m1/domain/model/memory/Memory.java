package com.memeboo2.haemi.m1.domain.model.memory;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 사진·글·음성을 하나의 추억으로 다루는 v3.0의 단일 피드 Aggregate.
 * FAMILY_ONLY는 저장 단계부터 어르신 피드 조회 대상이 될 수 없다.
 */
@Entity
@Table(name = "memory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memory {

    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_IMAGE_COUNT = 10;
    private static final int MAX_AUDIO_COUNT = 1;

    @Id
    @Column(name = "memory_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "author_user_id", nullable = false, columnDefinition = "uuid")
    private UUID authorUserId;

    @Column(name = "text_content", length = MAX_TEXT_LENGTH)
    private String textContent;

    @Column(name = "author_name", nullable = false, length = 50)
    private String authorName;

    @Column(name = "author_relation", nullable = false, length = 30)
    private String authorRelation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private MemoryModerationStatus moderationStatus;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<MemoryMedia> media = new ArrayList<>();

    public static Memory create(UUID groupId, UUID authorUserId, String textContent,
                                String authorName, String authorRelation,
                                MemoryVisibility visibility, MemoryModerationStatus moderationStatus) {
        Memory memory = new Memory();
        memory.id = UUID.randomUUID();
        memory.groupId = groupId;
        memory.authorUserId = authorUserId;
        memory.textContent = normalizeText(textContent);
        memory.authorName = authorName;
        memory.authorRelation = authorRelation;
        memory.visibility = visibility == null ? MemoryVisibility.GROUP_ALL : visibility;
        memory.moderationStatus = moderationStatus;
        memory.pinned = false;
        memory.createdAt = LocalDateTime.now();
        return memory;
    }

    public void addMedia(MemoryMediaType type, String storageKey, String thumbnailKey,
                         Long durationMs, int displayOrder) {
        if (type == null || storageKey == null || storageKey.isBlank()) {
            throw new MemoryValidationException("첨부 파일 정보가 올바르지 않아요.");
        }
        media.add(MemoryMedia.create(this, type, storageKey, thumbnailKey, durationMs, displayOrder));
    }

    public void validatePublishable() {
        if ((textContent == null || textContent.isBlank()) && media.isEmpty()) {
            throw new MemoryContentRequiredException();
        }
        long imageCount = media.stream().filter(item -> item.getType() == MemoryMediaType.IMAGE).count();
        long audioCount = media.stream().filter(item -> item.getType() == MemoryMediaType.AUDIO).count();
        if (imageCount > MAX_IMAGE_COUNT) {
            throw new MemoryValidationException("사진은 최대 10장까지 첨부할 수 있어요.");
        }
        if (audioCount > MAX_AUDIO_COUNT) {
            throw new MemoryValidationException("음성은 하나만 첨부할 수 있어요.");
        }
        if (moderationStatus == MemoryModerationStatus.BLOCKED) {
            throw new MemoryModerationException();
        }
    }

    public boolean isElderVisible() {
        return visibility == MemoryVisibility.GROUP_ALL
                && moderationStatus == MemoryModerationStatus.CLEAR;
    }

    public void approveModeration() {
        if (moderationStatus == MemoryModerationStatus.REVIEW) {
            moderationStatus = MemoryModerationStatus.CLEAR;
        }
    }

    public boolean canDelete(UUID actorId, UUID ownerId) {
        return authorUserId.equals(actorId) || ownerId.equals(actorId);
    }

    public List<MemoryMedia> getMedia() {
        return Collections.unmodifiableList(media);
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new MemoryValidationException("글은 최대 500자까지 작성할 수 있어요.");
        }
        return normalized;
    }
}
