package com.memeboo2.haemi.m1.domain.model.album;

import com.memeboo2.haemi.common.support.DomainIds;

import java.io.Serializable;
import java.util.UUID;

public record AlbumId(UUID value) implements Serializable {

    public static AlbumId newId() {
        return new AlbumId(UUID.randomUUID());
    }

    public static AlbumId of(String id) {
        return new AlbumId(DomainIds.parseUuid(id, "앨범 ID"));
    }

    public static AlbumId of(UUID id) {
        return new AlbumId(id);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
