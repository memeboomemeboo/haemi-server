package com.memeboo2.haemi.m0.infrastructure.security;

import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** AES-GCM envelope for the separately stored diagnosis field. */
@Component
public class ElderHealthCrypto {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public ElderHealthCrypto(@Value("${haemi.security.elder-health-encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("ELDER_HEALTH_ENCRYPTION_KEY 설정이 필요합니다.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey);
            if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
                throw new IllegalStateException("ELDER_HEALTH_ENCRYPTION_KEY는 Base64 인코딩된 AES 키여야 합니다.");
            }
            this.key = new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ELDER_HEALTH_ENCRYPTION_KEY는 Base64 형식이어야 합니다.", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new M0ValidationException("진단 정보는 비어 있을 수 없어요.");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("민감 건강 정보를 암호화할 수 없습니다.", e);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("invalid payload");
            }
            byte[] iv = Arrays.copyOf(payload, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("민감 건강 정보를 복호화할 수 없습니다.", e);
        }
    }
}
