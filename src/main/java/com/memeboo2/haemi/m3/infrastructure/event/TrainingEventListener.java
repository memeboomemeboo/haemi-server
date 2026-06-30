package com.memeboo2.haemi.m3.infrastructure.event;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m3.domain.event.HintRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingEventListener {

    private final NotificationPort notificationPort;

    @EventListener
    public void onHintRequested(HintRequestedEvent event) {
        log.info("손주 찬스 요청: sessionId={}, questionId={}", event.sessionId(), event.questionId());
        notificationPort.sendToGroup(
                Set.of("family-group-" + event.albumId()),
                "어르신이 힌트를 요청했습니다",
                "문제를 함께 풀 수 있도록 힌트를 보내주세요."
        );
    }
}
