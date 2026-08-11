package com.memeboo2.haemi.m1.domain.model.album;

import com.memeboo2.haemi.common.support.DomainIds;

import java.io.Serializable;
import java.util.UUID;

public record PhotoId(UUID value) implements Serializable {

    public static PhotoId newId() {
        return new PhotoId(UUID.randomUUID());
    }

    public static PhotoId of(String id) {
        return new PhotoId(DomainIds.parseUuid(id, "사진 ID"));
    }

    public static PhotoId of(UUID id) {
        return new PhotoId(id);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
