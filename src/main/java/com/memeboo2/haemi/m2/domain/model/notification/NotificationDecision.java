package com.memeboo2.haemi.m2.domain.model.notification;

public record NotificationDecision(boolean allowed, NotificationBlockReason reason) {

    public static NotificationDecision allow() {
        return new NotificationDecision(true, NotificationBlockReason.NONE);
    }

    public static NotificationDecision block(NotificationBlockReason reason) {
        return new NotificationDecision(false, reason);
    }

    public boolean blocked() {
        return !allowed;
    }
}
