package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.port.AiPoemGeneratorPort;
import com.memeboo2.haemi.m2.domain.port.ContentFilterPort;
import com.memeboo2.haemi.m2.domain.port.SttPort;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** F2-01 어르신 추억 알림 수신 정책 연동 검증. */
@ExtendWith(MockitoExtension.class)
class MemoryPostApplicationServiceTest {

    @Mock MemoryPostRepository postRepository;
    @Mock PhotoStoragePort photoStoragePort;
    @Mock ContentFilterPort contentFilterPort;
    @Mock AiPoemGeneratorPort aiPoemGeneratorPort;
    @Mock SttPort sttPort;
    @Mock NotificationPort notificationPort;

    MemoryPostApplicationService service;

    private final UUID albumId = UUID.randomUUID();
    private final Set<String> elders = Set.of("elder-1");

    @BeforeEach
    void setUp() {
        service = new MemoryPostApplicationService(
                postRepository, photoStoragePort, contentFilterPort,
                aiPoemGeneratorPort, sttPort, notificationPort);
        ReflectionTestUtils.setField(service, "elderDailyLimit", 3);
        // 야간 창을 빈 구간(0==0)으로 두어 시각과 무관하게 결정되도록 한다.
        ReflectionTestUtils.setField(service, "quietHoursStart", 0);
        ReflectionTestUtils.setField(service, "quietHoursEnd", 0);
    }

    private MemoryPost publishedPost() {
        MemoryPost post = MemoryPost.createDraft(
                albumId, AuthorInfo.of("m-1", "홍길동", "딸"), "보고싶어요", null, null);
        post.publish();
        return post;
    }

    @Test
    @DisplayName("게시글이고 한도 미만이면 어르신에게 알림을 발송한다")
    void sendsWhenAllowed() {
        MemoryPost post = publishedPost();
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.of(post));
        when(postRepository.countTodayNotificationsSentToElder(albumId)).thenReturn(0);

        service.handleElderNotification(post.getPostId().toString(), albumId, elders);

        verify(notificationPort).sendToGroup(eq(elders), anyString(), anyString());
    }

    @Test
    @DisplayName("일일 한도에 도달하면 알림을 발송하지 않는다")
    void skipsWhenDailyLimitReached() {
        MemoryPost post = publishedPost();
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.of(post));
        when(postRepository.countTodayNotificationsSentToElder(albumId)).thenReturn(3);

        service.handleElderNotification(post.getPostId().toString(), albumId, elders);

        verify(notificationPort, never()).sendToGroup(anySet(), anyString(), anyString());
    }

    @Test
    @DisplayName("발송 직전 상태 검증: 게시 상태가 아니면 알림을 발송하지 않는다")
    void skipsWhenPostNotPublished() {
        MemoryPost draft = MemoryPost.createDraft(
                albumId, AuthorInfo.of("m-1", "홍길동", "딸"), "임시글", null, null);
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.of(draft));

        service.handleElderNotification(draft.getPostId().toString(), albumId, elders);

        verify(notificationPort, never()).sendToGroup(anySet(), anyString(), anyString());
        verify(postRepository, never()).countTodayNotificationsSentToElder(any());
    }

    @Test
    @DisplayName("발송 직전 상태 검증: 글이 없으면 알림을 발송하지 않는다")
    void skipsWhenPostMissing() {
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.empty());

        service.handleElderNotification(UUID.randomUUID().toString(), albumId, elders);

        verify(notificationPort, never()).sendToGroup(anySet(), anyString(), anyString());
    }
}
