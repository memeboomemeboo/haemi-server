package com.memeboo2.haemi.auth.infrastructure.mail;

import com.memeboo2.haemi.auth.domain.port.VerificationEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class LogVerificationEmailAdapter implements VerificationEmailPort {
    @Override public void send(String recipient, String token) {
        log.info("[EMAIL-VERIFY] recipient={} token={}", recipient, token);
    }
}
