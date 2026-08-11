package com.memeboo2.haemi.auth.infrastructure.mail;

import com.memeboo2.haemi.auth.domain.port.VerificationEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class SmtpVerificationEmailAdapter implements VerificationEmailPort {
    private final JavaMailSender sender;
    private final VerificationEmailProperties properties;

    @Override
    public void send(String recipient, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(recipient);
        message.setSubject("[해미] 이메일 확인");
        message.setText("아래 링크를 24시간 안에 열어 이메일을 확인해주세요.\n"
                + properties.publicUrl() + "/api/v1/auth/email-verifications/confirm?token=" + token);
        sender.send(message);
    }
}
