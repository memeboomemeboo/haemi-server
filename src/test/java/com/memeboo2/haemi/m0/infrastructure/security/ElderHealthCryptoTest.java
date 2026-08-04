package com.memeboo2.haemi.m0.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElderHealthCryptoTest {

    private final ElderHealthCrypto crypto = new ElderHealthCrypto(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

    @Test
    void encryptsDiagnosisWithRandomizedAuthenticatedEncryption() {
        String first = crypto.encrypt("MILD");
        String second = crypto.encrypt("MILD");

        assertThat(first).isNotEqualTo("MILD").isNotEqualTo(second);
        assertThat(crypto.decrypt(first)).isEqualTo("MILD");
    }
}
