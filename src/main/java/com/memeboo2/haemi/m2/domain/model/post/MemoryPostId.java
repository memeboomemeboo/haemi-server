package com.memeboo2.haemi.m2.domain.model.post;

import com.memeboo2.haemi.common.support.DomainIds;

import java.io.Serializable;
import java.util.UUID;

public record MemoryPostId(UUID value) implements Serializable {

    public static MemoryPostId newId() {
        return new MemoryPostId(UUID.randomUUID());
    }

    public static MemoryPostId of(String id) {
        return new MemoryPostId(DomainIds.parseUuid(id, "추억 글 ID"));
    }

    public static MemoryPostId of(UUID id) {
        return new MemoryPostId(id);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
