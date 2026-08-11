package com.memeboo2.haemi.m4.presentation;

import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 포털 조회는 담당자별 분당 100건을 넘지 않도록 제한한다. */
@Component
public class InstitutionPortalRateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();

    public void check(UUID memberId) {
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(memberId, (ignored, current) -> {
            if (current == null || current.minute != minute) {
                return new Window(minute);
            }
            current.count.incrementAndGet();
            return current;
        });
        if (window.count.get() > MAX_REQUESTS_PER_MINUTE) {
            throw new M0AccessDeniedException("기관 포털 조회는 분당 100건까지 가능해요.");
        }
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
