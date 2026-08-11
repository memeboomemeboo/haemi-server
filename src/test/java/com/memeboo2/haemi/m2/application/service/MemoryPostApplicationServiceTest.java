package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m2.domain.model.post.AuthorInfo;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPost;
import com.memeboo2.haemi.m2.domain.model.post.MemoryPostId;
import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import com.memeboo2.haemi.m2.domain.model.post.ForbiddenWordException;
import com.memeboo2.haemi.m2.application.command.ReplyToPostCommand;
import com.memeboo2.haemi.m2.application.query.GeneratePoemDraftQuery;
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
import java.util.UUID;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock MemoryPostAiPersistenceService aiPersistenceService;
    @Mock NotificationPort notificationPort;
    @Mock com.memeboo2.haemi.m0.domain.port.ElderStatusQuery elderStatusQuery;

    MemoryPostApplicationService service;

    private final UUID albumId = UUID.randomUUID();
    private final String elderId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        service = new MemoryPostApplicationService(
                postRepository, photoStoragePort, contentFilterPort,
                aiPoemGeneratorPort, sttPort, aiPersistenceService, notificationPort, elderStatusQuery);
        lenient().when(elderStatusQuery.isDispatchable(elderId)).thenReturn(true);
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

        service.handleElderNotification(post.getPostId().toString(), albumId, elderId);

        verify(notificationPort).sendToElder(eq(elderId), anyString(), anyString());
    }

    @Test
    @DisplayName("일일 한도에 도달하면 알림을 발송하지 않는다")
    void skipsWhenDailyLimitReached() {
        MemoryPost post = publishedPost();
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.of(post));
        when(postRepository.countTodayNotificationsSentToElder(albumId)).thenReturn(3);

        service.handleElderNotification(post.getPostId().toString(), albumId, elderId);

        verify(notificationPort, never()).sendToElder(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("발송 직전 상태 검증: 게시 상태가 아니면 알림을 발송하지 않는다")
    void skipsWhenPostNotPublished() {
        MemoryPost draft = MemoryPost.createDraft(
                albumId, AuthorInfo.of("m-1", "홍길동", "딸"), "임시글", null, null);
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.of(draft));

        service.handleElderNotification(draft.getPostId().toString(), albumId, elderId);

        verify(notificationPort, never()).sendToElder(anyString(), anyString(), anyString());
        verify(postRepository, never()).countTodayNotificationsSentToElder(any());
    }

    @Test
    @DisplayName("EX-F201-05: 사별/입원 등 발송 불가 상태면 추억 알림을 발송하지 않는다")
    void skipsWhenElderNotDispatchable() {
        MemoryPost post = publishedPost();
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.of(post));
        when(elderStatusQuery.isDispatchable(elderId)).thenReturn(false);

        service.handleElderNotification(post.getPostId().toString(), albumId, elderId);

        verify(notificationPort, never()).sendToElder(anyString(), anyString(), anyString());
        verify(postRepository, never()).countTodayNotificationsSentToElder(any());
    }

    @Test
    @DisplayName("발송 직전 상태 검증: 글이 없으면 알림을 발송하지 않는다")
    void skipsWhenPostMissing() {
        when(postRepository.findById(any(MemoryPostId.class))).thenReturn(Optional.empty());

        service.handleElderNotification(UUID.randomUUID().toString(), albumId, elderId);

        verify(notificationPort, never()).sendToElder(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("시 초안은 게시글 원문을 실제 AI 포트에 전달한다")
    void generatesPoemThroughAiPort() {
        MemoryPost post = publishedPost();
        when(aiPersistenceService.loadPoemSource(post.getPostId().toString())).thenReturn("보고싶어요");
        when(aiPoemGeneratorPort.generatePoem("보고싶어요")).thenReturn("보고 싶은 마음이 피어납니다");

        String poem = service.generatePoemDraft(new GeneratePoemDraftQuery(post.getPostId().toString()));

        assertThat(poem).isEqualTo("보고 싶은 마음이 피어납니다");
        verify(aiPersistenceService).loadPoemSource(post.getPostId().toString());
        verify(aiPoemGeneratorPort).generatePoem("보고싶어요");
    }

    @Test
    @DisplayName("음성 답변은 STT 포트 전사 결과를 저장한다")
    void savesVoiceReplyFromSttTranscript() {
        MemoryPost post = publishedPost();
        when(sttPort.transcribe(any(), eq("audio/mpeg"), eq("reply.mp3"))).thenReturn("고마워요");
        when(aiPersistenceService.saveReply(post.getPostId().toString(), ReplyType.VOICE, "고마워요"))
                .thenReturn(com.memeboo2.haemi.m2.application.dto.MemoryPostResult.from(post));

        service.replyToPost(new ReplyToPostCommand(post.getPostId().toString(), elderId, ReplyType.VOICE,
                null, new ByteArrayInputStream(new byte[]{1, 2}), "audio/mpeg", "reply.mp3"));

        verify(aiPersistenceService).verifyCanReply(post.getPostId().toString());
        verify(aiPersistenceService).saveReply(post.getPostId().toString(), ReplyType.VOICE, "고마워요");
    }

    @Test
    @DisplayName("전사된 음성 답변도 가족 텍스트 글과 동일하게 금칙어를 검사한다")
    void rejectsForbiddenTranscriptBeforePersistence() {
        MemoryPost post = publishedPost();
        when(sttPort.transcribe(any(), anyString(), anyString())).thenReturn("욕설1");
        when(contentFilterPort.containsForbiddenWord("욕설1")).thenReturn(true);

        assertThatThrownBy(() -> service.replyToPost(new ReplyToPostCommand(
                post.getPostId().toString(), elderId, ReplyType.VOICE,
                null, new ByteArrayInputStream(new byte[]{1}), "audio/mpeg", "reply.mp3")))
                .isInstanceOf(ForbiddenWordException.class);

        verify(aiPersistenceService).verifyCanReply(post.getPostId().toString());
        verify(aiPersistenceService, never()).saveReply(anyString(), any(), anyString());
    }
}
