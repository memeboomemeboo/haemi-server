package com.memeboo2.haemi.offline.application;

public record BatchIngestResult(int acceptedCount, int duplicateCount) {

    public int total() {
        return acceptedCount + duplicateCount;
    }
}
