package com.memeboo2.haemi.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 기관 관리자로 가입할 수 있는 이메일 허용 목록 (#96).
 *
 * <p>비어 있으면 아무도 기관 관리자로 가입할 수 없다. 관리자 발급 API를 따로 만드는 대신
 * 운영이 배포 설정으로 통제한다.
 */
@ConfigurationProperties(prefix = "haemi.security.institution-admin")
public record InstitutionAdminProperties(List<String> allowedEmails) {

    public InstitutionAdminProperties {
        allowedEmails = allowedEmails == null ? List.of() : allowedEmails;
    }

    /** 가입 시 이메일을 소문자로 정규화하므로 비교 기준도 맞춘다. */
    public Set<String> normalizedAllowedEmails() {
        return allowedEmails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
