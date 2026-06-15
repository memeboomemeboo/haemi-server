package com.memeboo2.haemi.m1.domain.model.album;

import com.memeboo2.haemi.m1.domain.event.PhotoAnalysisCompletedEvent;
import com.memeboo2.haemi.m1.domain.event.PhotoUploadedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "albums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends AbstractAggregateRoot<Album> {

    private static final int MAX_GROUP_MEMBERS = 10;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_profile_id", nullable = false)
    private String elderProfileId;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "owner_member_id", nullable = false)
    private String ownerMemberId;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt DESC")
    private List<Photo> photos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "album_members", joinColumns = @JoinColumn(name = "album_id"))
    @Column(name = "member_id")
    private Set<String> memberIds = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Album create(String elderProfileId, String groupId, String ownerMemberId) {
        Album album = new Album();
        album.id = UUID.randomUUID();
        album.elderProfileId = elderProfileId;
        album.groupId = groupId;
        album.ownerMemberId = ownerMemberId;
        album.memberIds.add(ownerMemberId);
        album.createdAt = LocalDateTime.now();
        return album;
    }

    public AlbumId getAlbumId() {
        return AlbumId.of(id);
    }

    // F1-01, F1-02: 사진 추가 (중복 검사 포함)
    public Photo addPhoto(PhotoFile file, PhotoMetadata metadata, String hash, String uploadedBy) {
        if (isDuplicate(hash)) {
            throw new DuplicatePhotoException(hash);
        }
        Photo photo = Photo.create(file, metadata, hash, uploadedBy, this);
        this.photos.add(photo);
        registerEvent(new PhotoUploadedEvent(getAlbumId(), photo.getPhotoId(), uploadedBy, LocalDateTime.now()));
        return photo;
    }

    // F1-03: 가족 그룹 멤버 초대
    public void inviteMember(String memberId) {
        if (memberIds.size() >= MAX_GROUP_MEMBERS) {
            throw new AlbumMemberLimitExceededException(MAX_GROUP_MEMBERS);
        }
        memberIds.add(memberId);
    }

    public void removeMember(String memberId) {
        if (ownerMemberId.equals(memberId)) {
            throw new IllegalStateException("앨범 소유자는 탈퇴할 수 없습니다.");
        }
        memberIds.remove(memberId);
    }

    // F1-04: 사진 메모 업데이트
    public void updatePhotoMemo(PhotoId photoId, String timePeriod, String locationText, String memo) {
        Photo photo = findPhotoOrThrow(photoId);
        photo.updateMemo(timePeriod, locationText, memo);
    }

    // F1-04: 인물 태그 업데이트
    public void tagPersonsOnPhoto(PhotoId photoId, List<PersonTag> tags) {
        Photo photo = findPhotoOrThrow(photoId);
        photo.tagPersons(tags);
    }

    // 사진 삭제 (소유자 또는 본인 업로드만 가능)
    public void removePhoto(PhotoId photoId, String requestingMemberId) {
        Photo photo = findPhotoOrThrow(photoId);
        if (!ownerMemberId.equals(requestingMemberId)
                && !photo.getUploadedBy().equals(requestingMemberId)) {
            throw new PhotoDeleteForbiddenException();
        }
        photos.remove(photo);
    }

    // AI 분석 완료 처리
    public void markPhotoAnalysisCompleted(PhotoId photoId) {
        Photo photo = findPhotoOrThrow(photoId);
        photo.markAnalysisCompleted();
        registerEvent(new PhotoAnalysisCompletedEvent(getAlbumId(), photoId, LocalDateTime.now()));
    }

    public void markPhotoAnalysisFailed(PhotoId photoId) {
        findPhotoOrThrow(photoId).markAnalysisFailed();
    }

    // F1-05: AI 콘텐츠 생성 최소 조건 확인
    public boolean hasEnoughPhotosForAi(int minRequired) {
        return photos.stream()
                .filter(p -> p.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                .count() >= minRequired;
    }

    // F1-06: 타임라인용 사진 목록 (필터 조건 적용)
    public List<Photo> getPhotosForTimeline(String filterMemberId, String filterLocation) {
        return photos.stream()
                .filter(p -> filterMemberId == null || p.getPersonTags().stream()
                        .anyMatch(t -> t.getMemberId().equals(filterMemberId)))
                .filter(p -> filterLocation == null || (p.getMetadata().getLocationText() != null
                        && p.getMetadata().getLocationText().contains(filterLocation)))
                .sorted(Comparator.comparing(p -> {
                    LocalDateTime shot = p.getMetadata().getShotAt();
                    return shot != null ? shot : LocalDateTime.MIN;
                }))
                .toList();
    }

    public boolean isDuplicate(String hash) {
        return photos.stream().anyMatch(p -> p.getHash().equals(hash));
    }

    public boolean isMember(String memberId) {
        return memberIds.contains(memberId);
    }

    public List<Photo> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    public Set<String> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    private Photo findPhotoOrThrow(PhotoId photoId) {
        return photos.stream()
                .filter(p -> p.getId().equals(photoId.value()))
                .findFirst()
                .orElseThrow(() -> new PhotoNotFoundException(photoId));
    }
}
