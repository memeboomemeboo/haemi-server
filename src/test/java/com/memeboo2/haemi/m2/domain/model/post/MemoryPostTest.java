package com.memeboo2.haemi.m2.domain.model.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryPostTest {

    private final UUID albumId = UUID.randomUUID();
    private final AuthorInfo author = AuthorInfo.of("member-1", "홍길동", "딸");

    @Test
    @DisplayName("텍스트, 사진, 음성 중 하나라도 있으면 임시 게시물을 생성할 수 있다")
    void createDraft_acceptsAnyContentType() {
        MemoryPost text = MemoryPost.createDraft(albumId, author, "내용", null, null);
        MemoryPost photo = MemoryPost.createDraft(albumId, author, null, List.of("photo-key"), null);
        MemoryPost voice = MemoryPost.createDraft(albumId, author, null, null, "voice-key");

        assertThat(text.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(photo.getPhotoKeys()).containsExactly("photo-key");
        assertThat(voice.getVoiceMemoKey()).isEqualTo("voice-key");
    }

    @Test
    @DisplayName("모든 콘텐츠가 비어 있으면 게시물을 생성할 수 없다")
    void createDraft_rejectsEmptyContent() {
        assertThatThrownBy(() -> MemoryPost.createDraft(
                albumId, author, " ", List.of(), " "))
                .isInstanceOf(EmptyPostContentException.class);
    }

    @Test
    @DisplayName("본문 500자 및 사진 3개 제한을 검증한다")
    void createDraft_enforcesLengthAndPhotoCount() {
        assertThatThrownBy(() -> MemoryPost.createDraft(
                albumId, author, "가".repeat(501), null, null))
                .isInstanceOf(PostTextTooLongException.class);
        assertThatThrownBy(() -> MemoryPost.createDraft(
                albumId, author, null, List.of("1", "2", "3", "4"), null))
                .isInstanceOf(TooManyPhotosException.class);
    }

    @Test
    @DisplayName("게시하면 게시 상태와 게시 시각이 설정되고 재게시해도 시각이 바뀌지 않는다")
    void publish_isIdempotent() {
        MemoryPost post = draft();

        post.publish();
        var publishedAt = post.getPublishedAt();
        post.publish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("좋아요 토글은 좋아요 수와 인기도 점수에 반영된다")
    void toggleLike_updatesPopularity() {
        MemoryPost post = draft();

        post.toggleLike("member-2");
        assertThat(post.getLikeCount()).isEqualTo(1);
        assertThat(post.getPopularityScore()).isEqualTo(1);

        post.toggleLike("member-2");
        assertThat(post.getLikeCount()).isZero();
        assertThat(post.getPopularityScore()).isZero();
    }

    @Test
    @DisplayName("어르신 답변은 인기도 3점을 부여하며 한 번만 등록할 수 있다")
    void submitElderReply_updatesPopularityAndCannotRepeat() {
        MemoryPost post = draft();
        post.toggleLike("member-2");

        post.submitElderReply(ReplyType.VOICE, "반가워요");

        assertThat(post.hasElderReply()).isTrue();
        assertThat(post.getPopularityScore()).isEqualTo(4);
        assertThatThrownBy(() -> post.submitElderReply(ReplyType.EMOJI, "❤️"))
                .isInstanceOf(AlreadyRepliedException.class);
    }

    @Test
    @DisplayName("답변 유형별 내용을 검증한다: 빈 음성·초과 음성·미지원 이모지")
    void elderReply_validatesContent() {
        assertThatThrownBy(() -> ElderReply.of(ReplyType.VOICE, " "))
                .isInstanceOf(EmptyReplyContentException.class);
        assertThatThrownBy(() -> ElderReply.of(ReplyType.VOICE, "가".repeat(301)))
                .isInstanceOf(ReplyContentTooLongException.class);
        assertThatThrownBy(() -> ElderReply.of(ReplyType.EMOJI, "🐶"))
                .isInstanceOf(InvalidHeartEmojiException.class);
    }

    @Test
    @DisplayName("게시물은 작성자만 삭제할 수 있다")
    void delete_checksAuthor() {
        MemoryPost post = draft();

        assertThatThrownBy(() -> post.delete("other"))
                .isInstanceOf(PostDeleteForbiddenException.class);

        post.delete("member-1");
        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
    }

    @Test
    @DisplayName("외부에서 사진과 좋아요 컬렉션을 변경할 수 없다")
    void collections_areUnmodifiable() {
        MemoryPost post = draft();

        assertThatThrownBy(() -> post.getPhotoKeys().add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> post.getLikedMemberIds().add("member"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private MemoryPost draft() {
        return MemoryPost.createDraft(albumId, author, "내용", List.of("photo"), null);
    }
}
