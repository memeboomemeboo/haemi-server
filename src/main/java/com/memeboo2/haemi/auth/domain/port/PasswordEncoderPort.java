package com.memeboo2.haemi.auth.domain.port;

/**
 * 비밀번호 인코딩 포트.
 * 인프라에서 BCrypt 등으로 구현.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
