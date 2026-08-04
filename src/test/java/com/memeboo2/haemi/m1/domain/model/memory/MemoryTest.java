package com.memeboo2.haemi.m1.domain.model.memory;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryTest {

    @Test
    void emptyMemoryCannotBePublished() {
        Memory memory = Memory.create(UUID.randomUUID(), UUID.randomUUID(), null,
                "손녀", "GRANDDAUGHTER", MemoryVisibility.GROUP_ALL, MemoryModerationStatus.CLEAR);

        assertThatThrownBy(memory::validatePublishable)
                .isInstanceOf(MemoryContentRequiredException.class);
    }

    @Test
    void allowsAtMostTenImagesAndOneAudio() {
        Memory memory = Memory.create(UUID.randomUUID(), UUID.randomUUID(), null,
                "손녀", "GRANDDAUGHTER", MemoryVisibility.GROUP_ALL, MemoryModerationStatus.CLEAR);
        for (int i = 0; i < 11; i++) {
            memory.addMedia(MemoryMediaType.IMAGE, "image-" + i, null, null, i);
        }

        assertThatThrownBy(memory::validatePublishable)
                .isInstanceOf(MemoryValidationException.class)
                .hasMessage("사진은 최대 10장까지 첨부할 수 있어요.");
    }
}
