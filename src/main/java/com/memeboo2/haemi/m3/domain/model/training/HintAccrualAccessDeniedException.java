package com.memeboo2.haemi.m3.domain.model.training;

/**
 * 요청자가 해당 어르신의 가족 그룹 구성원이 아닐 때 손주 한마디 적립을 거부한다.
 */
public class HintAccrualAccessDeniedException extends RuntimeException {
    public HintAccrualAccessDeniedException() {
        super("해당 어르신의 가족 그룹 구성원만 손주 한마디를 적립할 수 있어요.");
    }
}
