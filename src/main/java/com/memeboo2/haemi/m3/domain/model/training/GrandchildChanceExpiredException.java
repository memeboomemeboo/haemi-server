package com.memeboo2.haemi.m3.domain.model.training;

public class GrandchildChanceExpiredException extends RuntimeException {
    public GrandchildChanceExpiredException() {
        super("손주 찬스 요청 후 30분이 지나 힌트를 전달할 수 없습니다.");
    }
}
