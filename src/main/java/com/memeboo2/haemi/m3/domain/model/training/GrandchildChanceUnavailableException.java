package com.memeboo2.haemi.m3.domain.model.training;

public class GrandchildChanceUnavailableException extends RuntimeException {
    public GrandchildChanceUnavailableException() {
        super("힌트를 요청할 가족 그룹 구성원이 없습니다.");
    }
}
