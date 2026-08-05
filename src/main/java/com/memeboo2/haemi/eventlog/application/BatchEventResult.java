package com.memeboo2.haemi.eventlog.application;

public record BatchEventResult(int acceptedCount, int duplicateCount, int rejectedCount) {

    public int total() {
        return acceptedCount + duplicateCount + rejectedCount;
    }
}
