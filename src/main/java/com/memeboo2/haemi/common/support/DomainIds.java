package com.memeboo2.haemi.common.support;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import java.util.UUID;

/**
 * 사용자 입력으로 들어온 문자열 식별자를 파싱한다.
 *
 * <p>{@link UUID#fromString}은 형식이 어긋나면 {@link IllegalArgumentException}과 함께
 * {@code Invalid UUID string: ...}이라는 내부 메시지를 던진다. 그대로 두면 클라이언트 잘못이
 * 서버 오류로 집계되거나 내부 메시지가 밖으로 새어 나간다.
 */
public final class DomainIds {

    private DomainIds() {
    }

    /**
     * @param label 사용자에게 보여줄 항목 이름 (예: {@code "앨범 ID"})
     */
    public static UUID parseUuid(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(label + "는 필수예요.");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException invalidFormat) {
            throw new DomainValidationException(label + "는 UUID 형식이어야 해요.", invalidFormat);
        }
    }
}
