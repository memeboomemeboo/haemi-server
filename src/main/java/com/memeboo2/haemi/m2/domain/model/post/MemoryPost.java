package com.memeboo2.haemi.m2.domain.model.post;

import com.memeboo2.haemi.m2.domain.event.ElderRepliedEvent;
import com.memeboo2.haemi.m2.domain.event.MemoryPostPublishedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "memory_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryPost extends AbstractAggregateRoot<MemoryPost> {

    // 어르신 답변 가중치(3) + 좋아요 가중치(1)
    private static final int REPLY_WEIGHT   = 3;
    private static final int LIKE_WEIGHT    = 1;
    private static final int MAX_PHOTOS     = 3;
    private static final int MAX_TEXT_LEN   = 500;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    /* ── 본문 콘텐츠 ── */
    @Column(name = "text_content", length = 500)
    private String textContent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_post_photos",
            joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "photo_key")
    private List<String> photoKeys = new ArrayList<>();

    @Column(name = "voice_memo_key")
    private String voiceMemoKey;

    /* ── 작성자 ── */
    @Embedded
    private AuthorInfo authorInfo;

    /* ── 어르신 답변 (nullable) ── */
    @Embedded
    private ElderReply elderReply;

    /* ── 좋아요 ── */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_post_likes",
            joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "member_id")
    private Set<String> likedMemberIds = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PostStatus status;

    @Column(name = "popularity_score")
    private int popularityScore;

    @Column(name = "is_read_by_elder")
    private boolean readByElder;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ── 팩토리: 임시 저장 ──
    public static MemoryPost createDraft(UUID albumId, AuthorInfo authorInfo,
                                         String textContent, List<String> photoKeys,
                                         String voiceMemoKey) {
        validateContentExists(textContent, photoKeys, voiceMemoKey);
        validateTextLength(textContent);
        validatePhotoCount(photoKeys);

        MemoryPost post = new MemoryPost();
        post.id            = UUID.randomUUID();
        post.albumId       = albumId;
        post.authorInfo    = authorInfo;
        post.textContent   = textContent;
        post.photoKeys     = photoKeys != null ? new ArrayList<>(photoKeys) : new ArrayList<>();
        post.voiceMemoKey  = voiceMemoKey;
        post.status        = PostStatus.DRAFT;
        post.popularityScore = 0;
        post.readByElder   = false;
        post.createdAt     = LocalDateTime.now();
        return post;
    }

    // ── 게시 (F2-01) ──
    public void publish() {
        if (this.status == PostStatus.PUBLISHED) return;
        this.status      = PostStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        registerEvent(new MemoryPostPublishedEvent(
                getPostId(), albumId, authorInfo, publishedAt));
    }

    // ── 어르신 답변 (F2-03) ──
    public void submitElderReply(ReplyType replyType, String content) {
        if (this.elderReply != null) throw new AlreadyRepliedException();
        this.elderReply = ElderReply.of(replyType, content);
        recalculatePopularity();
        registerEvent(new ElderRepliedEvent(getPostId(), albumId, replyType,
                this.elderReply.getRepliedAt()));
    }

    // ── 좋아요 토글 (F2-04) ──
    public void toggleLike(String memberId) {
        if (likedMemberIds.contains(memberId)) {
            likedMemberIds.remove(memberId);
        } else {
            likedMemberIds.add(memberId);
        }
        recalculatePopularity();
    }

    // ── 어르신 열람 처리 (F2-02) ──
    public void markReadByElder() {
        this.readByElder = true;
    }

    // ── 삭제 ──
    public void delete(String requestingMemberId) {
        if (!authorInfo.getMemberId().equals(requestingMemberId)) {
            throw new PostDeleteForbiddenException();
        }
        this.status = PostStatus.DELETED;
    }

    public MemoryPostId getPostId() {
        return MemoryPostId.of(id);
    }

    public boolean hasElderReply() {
        return elderReply != null;
    }

    public int getLikeCount() {
        return likedMemberIds.size();
    }

    public List<String> getPhotoKeys() {
        return Collections.unmodifiableList(photoKeys);
    }

    public Set<String> getLikedMemberIds() {
        return Collections.unmodifiableSet(likedMemberIds);
    }

    // ── 인기도 점수: 어르신 답변(×3) + 좋아요(×1) ──
    private void recalculatePopularity() {
        int replyScore = (elderReply != null) ? REPLY_WEIGHT : 0;
        this.popularityScore = replyScore + (likedMemberIds.size() * LIKE_WEIGHT);
    }

    private static void validateContentExists(String text, List<String> photos, String voice) {
        boolean hasText  = text  != null && !text.isBlank();
        boolean hasPhoto = photos != null && !photos.isEmpty();
        boolean hasVoice = voice != null && !voice.isBlank();
        if (!hasText && !hasPhoto && !hasVoice) throw new EmptyPostContentException();
    }

    private static void validateTextLength(String text) {
        if (text != null && text.length() > MAX_TEXT_LEN) {
            throw new PostTextTooLongException(text.length());
        }
    }

    private static void validatePhotoCount(List<String> photos) {
        if (photos != null && photos.size() > MAX_PHOTOS) {
            throw new TooManyPhotosException(photos.size());
        }
    }
}
