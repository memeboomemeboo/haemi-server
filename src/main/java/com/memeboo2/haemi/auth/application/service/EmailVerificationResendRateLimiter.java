package com.memeboo2.haemi.auth.application.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/** 공개 재발송 엔드포인트의 주소별 메일 폭탄 방지기. */
@Component
public class EmailVerificationResendRateLimiter {
    private static final int MAX_PER_HOUR = 5;
    private final ConcurrentHashMap<String, ArrayDeque<LocalDateTime>> attempts = new ConcurrentHashMap<>();

    public boolean allow(String email, LocalDateTime now) {
        ArrayDeque<LocalDateTime> timestamps = attempts.computeIfAbsent(email, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(now.minusHours(1))) {
                timestamps.removeFirst();
            }
            if ((!timestamps.isEmpty() && timestamps.peekLast().isAfter(now.minusMinutes(1)))
                    || timestamps.size() >= MAX_PER_HOUR) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
