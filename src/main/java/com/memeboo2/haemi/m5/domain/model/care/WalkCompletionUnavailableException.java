package com.memeboo2.haemi.m5.domain.model.care;

public class WalkCompletionUnavailableException extends RuntimeException {

    public WalkCompletionUnavailableException() {
        super("시작된 산책만 완료할 수 있습니다.");
    }
}
