package com.memeboo2.haemi.m2.infrastructure.event;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m2.application.service.MemoryPostApplicationService;
import com.memeboo2.haemi.m2.domain.event.ElderRepliedEvent;
import com.memeboo2.haemi.m2.domain.event.MemoryPostPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryPostEventListener {

    // 실제로는 AlbumRepository에서 앨범의 어르신 멤버 ID를 조회한다(현재는 플레이스홀더).
    private static final Set<String> ELDER_RECIPIENTS = Set.of("elder-device");

    private final NotificationPort notificationPort;
    private final MemoryPostApplicationService postService;

    // F2-01: 추억글 게시 → 어르신 알림. 일일 한도·야간 차단·상태 검증은 정책이 적용된다.
    @Async
    @EventListener
    public void onPostPublished(MemoryPostPublishedEvent event) {
        log.info("[EVENT] 추억글 게시됨: postId={}, albumId={}", event.postId(), event.albumId());
        postService.handleElderNotification(
                event.postId().toString(), event.albumId(), ELDER_RECIPIENTS);
    }

    // F2-03: 어르신 답변 → 가족 전체 알림
    @Async
    @EventListener
    public void onElderReplied(ElderRepliedEvent event) {
        log.info("[EVENT] 어르신 답변 완료: postId={}", event.postId());
        notificationPort.sendToGroup(
                Set.of("family-group"),
                "어르신 답장 도착",
                "어르신이 답장을 보내셨습니다 💌"
        );
    }

}
