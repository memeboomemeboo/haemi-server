package com.memeboo2.haemi.auth.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdMailConfigurationValidatorTest {

    @Test
    void rejectsMissingProductionMailSetting() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty("spring.mail.password", "");

        assertThatThrownBy(() -> new ProdMailConfigurationValidator(environment).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.mail.password");
    }

    @Test
    void acceptsCompleteProductionMailSetting() {
        assertThatCode(() -> new ProdMailConfigurationValidator(validEnvironment()).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.mail.host", "smtp.example.com")
                .withProperty("spring.mail.port", "587")
                .withProperty("spring.mail.username", "haemi")
                .withProperty("spring.mail.password", "secret")
                .withProperty("haemi.mail.from", "no-reply@example.com")
                .withProperty("haemi.mail.public-url", "https://api.example.com");
    }
}
