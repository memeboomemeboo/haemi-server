package com.memeboo2.haemi.m4.domain.model.dashboard;

public class DataInsufficientException extends RuntimeException {
    public DataInsufficientException() {
        super("데이터가 충분히 쌓이면 리포트가 제공됩니다(7일 이상 필요).");
    }
}
