package com.memeboo2.haemi.m4.domain.model.dashboard;

public class AlertRecipientsNotConfiguredException extends RuntimeException {

    public AlertRecipientsNotConfiguredException(String elderId) {
        super("알림 수신자를 설정해주세요. elderId=" + elderId);
    }
}
