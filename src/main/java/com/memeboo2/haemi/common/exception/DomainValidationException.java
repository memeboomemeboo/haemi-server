package com.memeboo2.haemi.common.exception;

/**
 * 사용자 입력이나 도메인 규칙 위반을 알리는 검증 예외.
 *
 * <p>메시지는 그대로 클라이언트에게 400으로 전달되므로, 사용자가 읽고 조치할 수 있는 문장만 담는다.
 * {@link IllegalArgumentException}을 상속하지 않는다. 상속하면 기존
 * {@code catch (IllegalArgumentException)} 블록들이 도메인 검증까지 삼켜서
 * 진짜 서버 오류와 구분하려는 목적이 무너진다.
 */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }

    public DomainValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
