package com.memeboo2.haemi.auth.domain.port;

public interface VerificationEmailPort {
    void send(String recipient, String token);
}
