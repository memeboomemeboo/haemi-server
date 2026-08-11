package com.memeboo2.haemi.m1.domain.model.reminiscence;

import com.memeboo2.haemi.common.support.DomainIds;

import java.io.Serializable;
import java.util.UUID;

public record ReminiscenceContentId(UUID value) implements Serializable {

    public static ReminiscenceContentId newId() {
        return new ReminiscenceContentId(UUID.randomUUID());
    }

    public static ReminiscenceContentId of(String id) {
        return new ReminiscenceContentId(DomainIds.parseUuid(id, "회상 콘텐츠 ID"));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
