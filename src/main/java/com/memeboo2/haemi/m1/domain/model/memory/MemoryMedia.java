package com.memeboo2.haemi.m1.domain.model.memory;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "memory_media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryMedia {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "memory_id", nullable = false)
    private Memory memory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemoryMediaType type;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "thumb_key", length = 255)
    private String thumbnailKey;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    static MemoryMedia create(Memory memory, MemoryMediaType type, String storageKey,
                              String thumbnailKey, Long durationMs, int displayOrder) {
        MemoryMedia media = new MemoryMedia();
        media.id = UUID.randomUUID();
        media.memory = memory;
        media.type = type;
        media.storageKey = storageKey;
        media.thumbnailKey = thumbnailKey;
        media.durationMs = durationMs;
        media.displayOrder = displayOrder;
        return media;
    }
}
