package com.memeboo2.haemi.m3.domain.model.training;

/** 실시간 힌트를 받은 가족 한 명만 응답할 수 있다. */
public class GrandchildChanceResponderNotRecipientException extends RuntimeException {

    public GrandchildChanceResponderNotRecipientException() {
        super("힌트를 요청받은 가족 구성원만 응답할 수 있습니다.");
    }
}
