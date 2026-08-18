package com.memeboo2.haemi.m0.application.command;

/** 어르신 평생 세션 silent refresh. 기기 바인딩을 함께 검증한다. */
public record RefreshElderSessionCommand(String refreshToken, String deviceId) {
}
