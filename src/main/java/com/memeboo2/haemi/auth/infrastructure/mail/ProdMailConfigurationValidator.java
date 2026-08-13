package com.memeboo2.haemi.auth.infrastructure.mail;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/** 운영에서 이메일 확인 링크를 조용히 잃지 않도록 SMTP 필수값을 기동 시점에 검증한다. */
@Component
@Profile("prod")
public class ProdMailConfigurationValidator implements InitializingBean {

    private static final List<String> REQUIRED_PROPERTIES = List.of(
            "spring.mail.host", "spring.mail.port", "spring.mail.username", "spring.mail.password",
            "haemi.mail.from", "haemi.mail.public-url");

    private final Environment environment;

    public ProdMailConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        for (String property : REQUIRED_PROPERTIES) {
            String value = environment.getProperty(property);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("prod 프로필에는 " + property + " 설정이 필요합니다.");
            }
        }
    }
}
