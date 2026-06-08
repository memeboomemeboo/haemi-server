package com.memeboo2.haemi.m1.domain.model.album;

import java.io.Serializable;
import java.util.UUID;

public record AlbumId(UUID value) implements Serializable {

    public static AlbumId newId() {
        return new AlbumId(UUID.randomUUID());
    }

    public static AlbumId of(String id) {
        return new AlbumId(UUID.fromString(id));
    }

    public static AlbumId of(UUID id) {
        return new AlbumId(id);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
