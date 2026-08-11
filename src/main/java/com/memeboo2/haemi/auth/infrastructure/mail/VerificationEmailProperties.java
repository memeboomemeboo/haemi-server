package com.memeboo2.haemi.auth.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "haemi.mail")
public record VerificationEmailProperties(String from, String publicUrl) {}
