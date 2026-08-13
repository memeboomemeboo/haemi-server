package com.memeboo2.haemi.m3.domain.model.training;

public class GrandchildChanceExpiredException extends RuntimeException {
    public GrandchildChanceExpiredException() {
        super("실시간 힌트 요청 후 60초가 지나 힌트를 전달할 수 없습니다.");
    }
}
