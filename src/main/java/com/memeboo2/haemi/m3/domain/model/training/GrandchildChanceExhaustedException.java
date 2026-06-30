package com.memeboo2.haemi.m3.domain.model.training;

public class GrandchildChanceExhaustedException extends RuntimeException {
    public GrandchildChanceExhaustedException() {
        super("오늘의 찬스를 모두 사용했습니다.");
    }
}
