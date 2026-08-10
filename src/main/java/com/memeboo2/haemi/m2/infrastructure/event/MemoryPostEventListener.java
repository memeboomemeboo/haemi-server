package com.memeboo2.haemi.m2.infrastructure.event;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m2.application.service.MemoryPostApplicationService;
import com.memeboo2.haemi.m2.domain.event.ElderRepliedEvent;
import com.memeboo2.haemi.m2.domain.event.MemoryPostPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryPostEventListener {

    private final NotificationPort notificationPort;
    private final MemoryPostApplicationService postService;
    private final AlbumRepository albums;
    private final ElderRecipientResolver elderRecipients;

    // F2-01: 추억글 게시 → 어르신 알림. 일일 한도·야간 차단·상태 검증은 정책이 적용된다.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostPublished(MemoryPostPublishedEvent event) {
        log.info("[EVENT] 추억글 게시됨: postId={}, albumId={}", event.postId(), event.albumId());
        albums.findById(AlbumId.of(event.albumId()))
                .ifPresentOrElse(album -> elderRecipients.resolveByGroupId(album.getGroupId())
                                .ifPresentOrElse(elderId -> postService.handleElderNotification(
                                                event.postId().toString(), event.albumId(), elderId),
                                        () -> log.warn("[EVENT] 연결된 어르신 프로필이 없어 알림을 생략합니다. groupId={}",
                                                album.getGroupId())),
                        () -> log.warn("[EVENT] 알림 대상 앨범이 없어 어르신 알림을 생략합니다. albumId={}", event.albumId()));
    }

    // F2-03: 어르신 답변 → 가족 전체 알림
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onElderReplied(ElderRepliedEvent event) {
        log.info("[EVENT] 어르신 답변 완료: postId={}", event.postId());
        albums.findById(AlbumId.of(event.albumId()))
                .ifPresentOrElse(album -> notificationPort.sendToGroup(
                                album.getMemberIds(),
                                "어르신 답장 도착",
                                "어르신이 답장을 보내셨습니다 💌"),
                        () -> log.warn("[EVENT] 알림 대상 앨범이 없어 가족 알림을 생략합니다. albumId={}", event.albumId()));
    }

}
