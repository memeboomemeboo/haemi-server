package com.memeboo2.haemi.m4.domain.model.dashboard;

public class ReportDeliveryBlockedException extends RuntimeException {
    public ReportDeliveryBlockedException() {
        super("현재 상태에서는 리포트를 발송하지 않습니다.");
    }
}
