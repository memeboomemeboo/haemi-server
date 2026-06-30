package com.memeboo2.haemi.auth.domain.model;

/**
 * 기관 관리자 2FA 필수이나 TOTP 코드가 누락/불일치 시 발생.
 */
public class TotpRequiredException extends RuntimeException {
    public TotpRequiredException(String message) {
        super(message);
    }
}
