package com.memeboo2.haemi.m2.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m2.application.command.*;
import com.memeboo2.haemi.m2.application.dto.FeedResult;
import com.memeboo2.haemi.m2.application.dto.MemoryPostResult;
import com.memeboo2.haemi.m2.application.query.GeneratePoemDraftQuery;
import com.memeboo2.haemi.m2.application.query.GetFeedQuery;
import com.memeboo2.haemi.m2.domain.model.notification.ElderNotificationPolicy;
import com.memeboo2.haemi.m2.domain.model.notification.NotificationDecision;
import com.memeboo2.haemi.m2.domain.model.notification.QuietHours;
import com.memeboo2.haemi.m2.domain.model.post.*;
import com.memeboo2.haemi.m2.domain.port.AiPoemGeneratorPort;
import com.memeboo2.haemi.m2.domain.port.ContentFilterPort;
import com.memeboo2.haemi.m2.domain.port.SttPort;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryPostApplicationService {

    private final MemoryPostRepository postRepository;
    private final PhotoStoragePort photoStoragePort;
    private final ContentFilterPort contentFilterPort;
    private final AiPoemGeneratorPort aiPoemGeneratorPort;
    private final SttPort sttPort;
    private final NotificationPort notificationPort;

    // F2-01: 어르신 알림 정책 — 일일 한도(기본 3회) + 야간 차단(기본 21시~08시)
    @Value("${haemi.notification.elder-daily-limit:3}")
    private int elderDailyLimit;

    @Value("${haemi.notification.elder-quiet-hours-start:21}")
    private int quietHoursStart;

    @Value("${haemi.notification.elder-quiet-hours-end:8}")
    private int quietHoursEnd;

    private ElderNotificationPolicy notificationPolicy() {
        return new ElderNotificationPolicy(
                elderDailyLimit, new QuietHours(quietHoursStart, quietHoursEnd));
    }

    // F2-01: 추억글 작성 (임시저장 or 즉시게시)
    @Transactional
    public MemoryPostResult createPost(CreateMemoryPostCommand command) {
        // 금칙어 검사
        if (command.textContent() != null
                && contentFilterPort.containsForbiddenWord(command.textContent())) {
            throw new ForbiddenWordException();
        }

        // 사진 업로드
        List<String> photoKeys = new ArrayList<>();
        if (command.photos() != null) {
            for (CreateMemoryPostCommand.PhotoAttachment photo : command.photos()) {
                try {
                    String key = photoStoragePort.store(
                            photo.inputStream(), photo.originalFilename(), photo.contentType());
                    photoKeys.add(key);
                } catch (Exception e) {
                    throw new PostStorageException("사진 저장에 실패했습니다.", e);
                }
            }
        }

        // 음성 메모 업로드
        String voiceKey = null;
        if (command.voiceMemo() != null) {
            try {
                voiceKey = photoStoragePort.store(
                        command.voiceMemo().inputStream(), "voice.m4a",
                        command.voiceMemo().contentType());
            } catch (Exception e) {
                throw new PostStorageException("음성 저장에 실패했습니다.", e);
            }
        }

        AuthorInfo authorInfo = AuthorInfo.of(
                command.memberId(), command.memberName(), command.relation());
        MemoryPost post = MemoryPost.createDraft(
                UUID.fromString(command.albumId()), authorInfo,
                command.textContent(), photoKeys, voiceKey);

        if (command.publishImmediately()) {
            post.publish();
        }

        postRepository.save(post);
        log.info("추억글 생성: postId={}, status={}", post.getPostId(), post.getStatus());
        return MemoryPostResult.from(post);
    }

    // F2-01: 임시저장 → 게시
    @Transactional
    public MemoryPostResult publishDraft(PublishDraftCommand command) {
        MemoryPost post = loadPostOrThrow(command.postId());
        if (!post.getAuthorInfo().getMemberId().equals(command.memberId())) {
            throw new PostDeleteForbiddenException();
        }
        if (post.getTextContent() != null
                && contentFilterPort.containsForbiddenWord(post.getTextContent())) {
            throw new ForbiddenWordException();
        }
        post.publish();
        postRepository.save(post);
        return MemoryPostResult.from(post);
    }

    // F2-01: 어르신 추억 알림 수신 처리 (이벤트 리스너에서 호출)
    // 일일 3회 한도 + 야간 21:00~08:00 차단 + 발송 직전 상태 검증.
    // 사별·입원 상태 차단(EX-F201-05)은 어르신 상태 머신(#36) 의존이라 여기서 다루지 않는다(#50 이월).
    @Transactional
    public void handleElderNotification(String postId, UUID albumId, Set<String> elderMemberIds) {
        // 발송 직전 상태 검증: 글이 존재하고 게시 상태여야 한다.
        MemoryPost post = postRepository.findById(MemoryPostId.of(postId)).orElse(null);
        if (post == null || post.getStatus() != PostStatus.PUBLISHED) {
            log.info("발송 직전 상태 검증 실패로 알림 생략: postId={}, status={}",
                    postId, post != null ? post.getStatus() : "NOT_FOUND");
            return;
        }

        int todayCount = postRepository.countTodayNotificationsSentToElder(albumId);
        NotificationDecision decision = notificationPolicy().decide(todayCount, LocalTime.now());
        if (decision.blocked()) {
            log.info("어르신 알림 차단({}) → 저녁 요약으로 이월: albumId={}, postId={}",
                    decision.reason(), albumId, postId);
            // 차단분은 EveningNotificationScheduler의 저녁 요약으로 묶여 전달된다.
            return;
        }

        String preview = buildPreview(post);
        String title   = post.getAuthorInfo().getMemberName() + "("
                + post.getAuthorInfo().getRelation() + ")님의 추억글";
        notificationPort.sendToGroup(elderMemberIds, title, preview);
        log.info("어르신 알림 발송: postId={}", postId);
    }

    // F2-02: 어르신 열람 처리
    @Transactional
    public void markReadByElder(String postId) {
        MemoryPost post = loadPostOrThrow(postId);
        post.markReadByElder();
        postRepository.save(post);
    }

    // F2-03: 어르신 답변 — STT 변환 후 시/짧은 글
    @Transactional
    public MemoryPostResult replyToPost(ReplyToPostCommand command) {
        MemoryPost post = loadPostOrThrow(command.postId());

        String content = command.textContent();

        // 음성 입력이 있으면 STT 변환
        if (command.voiceInputStream() != null
                && (command.replyType() == ReplyType.POEM
                    || command.replyType() == ReplyType.SHORT_TEXT)) {
            content = sttPort.transcribe(command.voiceInputStream(), command.voiceContentType());
        }

        // IMAGE 유형이면 storageKey 또는 이모지 코드를 content로
        if (command.replyType() == ReplyType.IMAGE) {
            content = command.imageKeyOrEmoji();
        }

        post.submitElderReply(command.replyType(), content);
        postRepository.save(post);
        log.info("어르신 답변 완료: postId={}, type={}", command.postId(), command.replyType());
        return MemoryPostResult.from(post);
    }

    // F2-03: AI 시 초안 생성
    @Transactional(readOnly = true)
    public String generatePoemDraft(GeneratePoemDraftQuery query) {
        MemoryPost post = loadPostOrThrow(query.postId());
        String baseText = post.getTextContent() != null ? post.getTextContent() : "소중한 추억";
        return aiPoemGeneratorPort.generatePoem(baseText);
    }

    // F2-04: 좋아요 토글
    @Transactional
    public MemoryPostResult toggleLike(LikePostCommand command) {
        MemoryPost post = loadPostOrThrow(command.postId());
        post.toggleLike(command.memberId());
        postRepository.save(post);
        return MemoryPostResult.from(post);
    }

    // F2-04: 공동 피드 조회
    @Transactional(readOnly = true)
    public FeedResult getFeed(GetFeedQuery query) {
        UUID albumId = UUID.fromString(query.albumId());
        int offset   = query.page() * query.size();

        List<MemoryPostResult> posts;
        if ("POPULAR".equalsIgnoreCase(query.sortBy())) {
            posts = postRepository
                    .findPublishedByAlbumIdOrderByPopularityDesc(albumId, offset, query.size())
                    .stream().map(MemoryPostResult::from).toList();
        } else {
            posts = postRepository
                    .findPublishedByAlbumIdOrderByPublishedAtDesc(albumId, offset, query.size())
                    .stream().map(MemoryPostResult::from).toList();
        }

        long total = postRepository.countPublishedByAlbumId(albumId);
        return new FeedResult(posts, total, query.page(), query.size(),
                (long) (query.page() + 1) * query.size() < total);
    }

    // 삭제
    @Transactional
    public void deletePost(DeletePostCommand command) {
        MemoryPost post = loadPostOrThrow(command.postId());
        post.delete(command.memberId());
        postRepository.save(post);
    }

    private MemoryPost loadPostOrThrow(String postId) {
        return postRepository.findById(MemoryPostId.of(postId))
                .orElseThrow(() -> new MemoryPostNotFoundException(postId));
    }

    private String buildPreview(MemoryPost post) {
        if (post.getTextContent() != null && !post.getTextContent().isBlank()) {
            String text = post.getTextContent();
            return text.length() > 50 ? text.substring(0, 50) + "…" : text;
        }
        if (!post.getPhotoKeys().isEmpty()) return "사진을 보내셨어요 📸";
        if (post.getVoiceMemoKey() != null) return "음성 메시지를 보내셨어요 🎵";
        return "추억글이 도착했습니다.";
    }
}
