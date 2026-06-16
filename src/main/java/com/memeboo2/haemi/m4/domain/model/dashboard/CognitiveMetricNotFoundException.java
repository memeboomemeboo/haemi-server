package com.memeboo2.haemi.m4.domain.model.dashboard;

public class CognitiveMetricNotFoundException extends RuntimeException {
    public CognitiveMetricNotFoundException(String elderId) {
        super("인지 변화 지표를 찾을 수 없습니다: " + elderId);
    }
}
