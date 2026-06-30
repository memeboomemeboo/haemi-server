package com.memeboo2.haemi.m3.domain.model.training;

public class DailySessionAlreadyCompletedException extends RuntimeException {
    public DailySessionAlreadyCompletedException(String elderId) {
        super("오늘의 훈련을 완료했습니다: " + elderId);
    }
}
