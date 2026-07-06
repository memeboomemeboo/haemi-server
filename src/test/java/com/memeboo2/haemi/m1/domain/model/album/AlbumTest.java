package com.memeboo2.haemi.m1.domain.model.album;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlbumTest {

    private Album album;

    @BeforeEach
    void setUp() {
        album = Album.create("elder-1", "group-1", "owner");
    }

    @Test
    @DisplayName("앨범 생성 시 소유자가 첫 구성원으로 등록된다")
    void create_registersOwnerAsMember() {
        assertThat(album.getAlbumId()).isNotNull();
        assertThat(album.isMember("owner")).isTrue();
        assertThat(album.getMemberIds()).containsExactly("owner");
    }

    @Test
    @DisplayName("사진을 추가하면 PENDING 상태로 앨범에 포함된다")
    void addPhoto_addsPendingPhoto() {
        Photo photo = addPhoto("hash-1", "uploader", LocalDateTime.of(2026, 1, 1, 12, 0));

        assertThat(album.getPhotos()).containsExactly(photo);
        assertThat(photo.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
        assertThat(photo.getUploadedBy()).isEqualTo("uploader");
    }

    @Test
    @DisplayName("동일한 해시의 사진은 중복 등록할 수 없다")
    void addPhoto_rejectsDuplicateHash() {
        addPhoto("same-hash", "uploader", LocalDateTime.now());

        assertThatThrownBy(() -> addPhoto("same-hash", "uploader", LocalDateTime.now()))
                .isInstanceOf(DuplicatePhotoException.class);
    }

    @Test
    @DisplayName("지원하지 않는 포맷과 20MB 초과 파일을 거부한다")
    void photoFile_validatesTypeAndSize() {
        assertThatThrownBy(() -> PhotoFile.of("key", "photo.gif", "image/gif", 100))
                .isInstanceOf(UnsupportedPhotoFormatException.class);
        assertThatThrownBy(() -> PhotoFile.of(
                "key", "photo.jpg", "image/jpeg", 20L * 1024 * 1024 + 1))
                .isInstanceOf(PhotoFileSizeExceededException.class);
    }

    @Test
    @DisplayName("앨범 구성원은 최대 10명까지 초대할 수 있다")
    void inviteMember_enforcesMemberLimit() {
        for (int i = 1; i <= 9; i++) {
            album.inviteMember("member-" + i);
        }

        assertThat(album.getMemberIds()).hasSize(10);
        assertThatThrownBy(() -> album.inviteMember("member-10"))
                .isInstanceOf(AlbumMemberLimitExceededException.class);
    }

    @Test
    @DisplayName("앨범 소유자는 구성원에서 제거할 수 없다")
    void removeMember_rejectsOwner() {
        assertThatThrownBy(() -> album.removeMember("owner"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("사진 메모와 인물 태그를 수정한다")
    void updatePhotoMetadata_updatesMemoAndTags() {
        Photo photo = addPhoto("hash", "uploader", LocalDateTime.now());

        album.updatePhotoMemo(photo.getPhotoId(), "유년기", "서울", "가족 여행");
        album.tagPersonsOnPhoto(photo.getPhotoId(), List.of(
                PersonTag.of("member-1", "홍길동")));

        assertThat(photo.getMetadata().getTimePeriod()).isEqualTo("유년기");
        assertThat(photo.getMetadata().getLocationText()).isEqualTo("서울");
        assertThat(photo.getMetadata().getMemo()).isEqualTo("가족 여행");
        assertThat(photo.getPersonTags()).extracting(PersonTag::getMemberId)
                .containsExactly("member-1");
    }

    @Test
    @DisplayName("메모 200자 및 인물 태그 10개 제한을 검증한다")
    void updatePhotoMetadata_enforcesLimits() {
        Photo photo = addPhoto("hash", "uploader", LocalDateTime.now());

        assertThatThrownBy(() -> album.updatePhotoMemo(
                photo.getPhotoId(), null, null, "가".repeat(201)))
                .isInstanceOf(MemoLengthExceededException.class);

        List<PersonTag> tags = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(i -> PersonTag.of("member-" + i, "이름" + i))
                .toList();
        assertThatThrownBy(() -> album.tagPersonsOnPhoto(photo.getPhotoId(), tags))
                .isInstanceOf(PersonTagLimitExceededException.class);
    }

    @Test
    @DisplayName("사진은 소유자 또는 업로더만 삭제할 수 있다")
    void removePhoto_checksPermission() {
        Photo photo = addPhoto("hash", "uploader", LocalDateTime.now());

        assertThatThrownBy(() -> album.removePhoto(photo.getPhotoId(), "stranger"))
                .isInstanceOf(PhotoDeleteForbiddenException.class);

        album.removePhoto(photo.getPhotoId(), "uploader");
        assertThat(album.getPhotos()).isEmpty();
    }

    @Test
    @DisplayName("분석 완료 사진 수로 AI 콘텐츠 생성 가능 여부를 판단한다")
    void hasEnoughPhotosForAi_countsOnlyCompletedPhotos() {
        Photo completed = addPhoto("hash-1", "uploader", LocalDateTime.now());
        addPhoto("hash-2", "uploader", LocalDateTime.now());

        album.markPhotoAnalysisCompleted(completed.getPhotoId());

        assertThat(album.hasEnoughPhotosForAi(1)).isTrue();
        assertThat(album.hasEnoughPhotosForAi(2)).isFalse();
    }

    @Test
    @DisplayName("타임라인 사진을 인물과 장소로 필터링하고 촬영 시각순으로 정렬한다")
    void getPhotosForTimeline_filtersAndSorts() {
        Photo later = addPhoto("later", "uploader", LocalDateTime.of(2026, 2, 1, 0, 0));
        Photo earlier = addPhoto("earlier", "uploader", LocalDateTime.of(2025, 1, 1, 0, 0));
        album.updatePhotoMemo(later.getPhotoId(), null, "서울 강남", null);
        album.updatePhotoMemo(earlier.getPhotoId(), null, "서울 종로", null);
        album.tagPersonsOnPhoto(later.getPhotoId(), List.of(PersonTag.of("member-1", "가족")));
        album.tagPersonsOnPhoto(earlier.getPhotoId(), List.of(PersonTag.of("member-1", "가족")));

        assertThat(album.getPhotosForTimeline("member-1", "서울"))
                .containsExactly(earlier, later);
        assertThat(album.getPhotosForTimeline("unknown", null)).isEmpty();
    }

    @Test
    @DisplayName("외부에서 사진 및 구성원 컬렉션을 변경할 수 없다")
    void collections_areUnmodifiable() {
        assertThatThrownBy(() -> album.getPhotos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> album.getMemberIds().add("member"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Photo addPhoto(String hash, String uploader, LocalDateTime shotAt) {
        return album.addPhoto(
                PhotoFile.of("key-" + hash, hash + ".jpg", "image/jpeg", 1024),
                PhotoMetadata.of(shotAt, 37.5, 127.0),
                hash,
                uploader);
    }
}
