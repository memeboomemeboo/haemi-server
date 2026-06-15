package com.memeboo2.haemi.m2.infrastructure.event;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m2.domain.event.BadgeAwardedEvent;
import com.memeboo2.haemi.m2.domain.event.ElderRepliedEvent;
import com.memeboo2.haemi.m2.domain.event.MemoryPostPublishedEvent;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
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

    private final NotificationPort notificationPort;
    private final MemoryPostRepository postRepository;

    // F2-02: 추억글 게시 → 어르신 알림 (실제로는 AlbumRepository에서 elderMemberId 조회)
    @Async
    @EventListener
    public void onPostPublished(MemoryPostPublishedEvent event) {
        log.info("[EVENT] 추억글 게시됨: postId={}, albumId={}", event.postId(), event.albumId());
        // 알림은 MemoryPostApplicationService.handleElderNotification() 에서 한도 체크 후 처리
        // 여기서는 간단하게 로그만 출력 (실제로는 어르신 멤버 ID를 조회해서 알림 발송)
        notificationPort.sendToGroup(
                Set.of("elder-device"),
                event.authorInfo().getMemberName() + "(" + event.authorInfo().getRelation() + ")님의 추억글",
                "새 추억글이 도착했습니다."
        );
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

    // F2-04: 뱃지 수여 → 그룹 전체 알림
    @Async
    @EventListener
    public void onBadgeAwarded(BadgeAwardedEvent event) {
        log.info("[EVENT] 뱃지 수여: member={}, badge={}",
                event.memberName(), event.badgeGrade().getLabel());
        notificationPort.sendToGroup(
                Set.of("family-group"),
                "🏅 뱃지 획득!",
                event.memberName() + "님이 '" + event.badgeGrade().getLabel() + "' 뱃지를 획득했습니다!"
        );
    }
}
