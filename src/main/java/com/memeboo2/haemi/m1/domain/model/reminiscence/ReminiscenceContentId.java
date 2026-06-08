package com.memeboo2.haemi.m1.domain.model.reminiscence;

import java.io.Serializable;
import java.util.UUID;

public record ReminiscenceContentId(UUID value) implements Serializable {

    public static ReminiscenceContentId newId() {
        return new ReminiscenceContentId(UUID.randomUUID());
    }

    public static ReminiscenceContentId of(String id) {
        return new ReminiscenceContentId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
