package com.memeboo2.haemi.m3.domain.model.training;

public class GrandchildChanceResponderNotMemberException extends RuntimeException {

    public GrandchildChanceResponderNotMemberException() {
        super("해당 가족 그룹의 구성원만 힌트를 전달할 수 있습니다.");
    }
}
