package com.memeboo2.haemi.m3.domain.model.training;

/**
 * EX-F301-10: 사별/입원 등 부적합 상태의 어르신은 인지 훈련 세션을 개시할 수 없다.
 */
public class SessionStartBlockedByElderStatusException extends RuntimeException {
    public SessionStartBlockedByElderStatusException(String elderId) {
        super("현재 상태에서는 인지 훈련 세션을 시작할 수 없어요: " + elderId);
    }
}
