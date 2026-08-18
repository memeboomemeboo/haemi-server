package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 어르신 전화번호를 식별·중복 감지용 해시로만 변환한다 (F0-01-E).
 * 원본 번호는 저장하지 않으며, OTP 인증도 하지 않는다.
 */
@Component
public class PhoneHasher {

    public String hash(String phoneNumber) {
        String digits = phoneNumber == null ? "" : phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 9 || digits.length() > 11) {
            throw new M0ValidationException("전화번호를 다시 확인해주세요.");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(digits.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("전화번호를 안전하게 처리할 수 없습니다.", e);
        }
    }
}
