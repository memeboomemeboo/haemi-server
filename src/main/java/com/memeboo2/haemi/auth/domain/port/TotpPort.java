package com.memeboo2.haemi.auth.domain.port;

/**
 * TOTP(Time-based One-Time Password) 2FA 포트.
 * 기관 관리자 로그인 시 필수.
 */
public interface TotpPort {

    /** 새 비밀키 생성 */
    String generateSecret();

    /** QR 코드 otpauth URI 생성 */
    String generateQrUri(String secret, String email);

    /** TOTP 코드 검증 */
    boolean verifyCode(String secret, String code);
}
