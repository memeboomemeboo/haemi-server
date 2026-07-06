package com.memeboo2.haemi.m3.infrastructure.event;

import com.memeboo2.haemi.m3.domain.event.GrandchildChanceUnusedBadgeAwardedEvent;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m3.domain.event.HintRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingEventListener {

    private final NotificationPort notificationPort;

    @EventListener
    public void onHintRequested(HintRequestedEvent event) {
        if (event.recipientMemberIds().isEmpty()) {
            log.warn("손주 찬스 요청 알림 생략: sessionId={}, questionId={}, reason=no-recipient",
                    event.sessionId(), event.questionId());
            return;
        }
        log.info("손주 찬스 요청: sessionId={}, questionId={}, recipients={}",
                event.sessionId(), event.questionId(), event.recipientMemberIds().size());
        notificationPort.sendToGroup(
                event.recipientMemberIds(),
                "어르신이 힌트를 요청했습니다",
                "문제를 함께 풀 수 있도록 힌트를 보내주세요."
        );
    }

    @EventListener
    public void onGrandchildChanceUnusedBadgeAwarded(GrandchildChanceUnusedBadgeAwardedEvent event) {
        log.info("손주 찬스 미사용 완료 뱃지 수여: sessionId={}, elderId={}",
                event.sessionId(), event.elderId());
    }
}
