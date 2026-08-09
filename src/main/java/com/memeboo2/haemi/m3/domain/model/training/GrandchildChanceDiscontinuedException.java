package com.memeboo2.haemi.m3.domain.model.training;

/**
 * F3-03(P4/v3.0): 실시간 소진형 "손주 찬스"는 폐기되었다.
 * 사전 적립형(POST /{sessionId}/hints/served)만 사용한다.
 */
public class GrandchildChanceDiscontinuedException extends RuntimeException {
    public GrandchildChanceDiscontinuedException() {
        super("실시간 손주 찬스는 더 이상 지원하지 않아요. 손주 한마디 즉시 제공을 사용해 주세요.");
    }
}
