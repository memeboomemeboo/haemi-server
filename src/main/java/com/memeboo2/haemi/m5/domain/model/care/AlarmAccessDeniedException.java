package com.memeboo2.haemi.m5.domain.model.care;

public class AlarmAccessDeniedException extends RuntimeException {

    public AlarmAccessDeniedException() {
        super("해당 어르신의 알람만 확인할 수 있습니다.");
    }
}
