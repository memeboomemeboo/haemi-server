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
import java.util.stream.Collectors;

@Entity
@Table(name = "albums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends AbstractAggregateRoot<Album> {

    private static final int MAX_GROUP_MEMBERS = 10;
    private static final int MIN_PHOTOS_FOR_TIMELINE = 3;

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
    @Getter(AccessLevel.NONE)
    private List<AlbumMember> members = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Album create(String elderProfileId, String groupId, String ownerMemberId) {
        Album album = new Album();
        album.id = UUID.randomUUID();
        album.elderProfileId = elderProfileId;
        album.groupId = groupId;
        album.ownerMemberId = ownerMemberId;
        album.members.add(AlbumMember.accepted(ownerMemberId));
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

    // F1-03: 가족 그룹 멤버 초대 (수락 전까지는 PENDING 상태). 새로 초대된 경우에만 true를 반환한다.
    public boolean inviteMember(String memberId) {
        Optional<AlbumMember> existing = findMember(memberId);
        if (existing.isPresent()) {
            existing.get().refreshInviteIfPending();
            return false;
        }
        if (members.size() >= MAX_GROUP_MEMBERS) {
            throw new AlbumMemberLimitExceededException(MAX_GROUP_MEMBERS);
        }
        members.add(AlbumMember.pending(memberId));
        return true;
    }

    // F1-03: 초대 수락 (24시간 경과 시 만료)
    public void acceptInvite(String memberId) {
        AlbumMember member = findMember(memberId)
                .orElseThrow(() -> new MemberNotInvitedException(memberId));
        if (member.getStatus() == MembershipStatus.ACCEPTED) {
            return;
        }
        if (member.isExpired()) {
            throw new InviteExpiredException();
        }
        member.accept();
    }

    public void removeMember(String memberId) {
        if (ownerMemberId.equals(memberId)) {
            throw new IllegalStateException("앨범 소유자는 탈퇴할 수 없습니다.");
        }
        members.removeIf(m -> m.getMemberId().equals(memberId));
    }

    // F1-03: 역할 기반 접근 제어 - 앨범 그룹 구성원만 허용
    public void requireMember(String memberId) {
        if (!isMember(memberId)) {
            throw new AlbumAccessDeniedException();
        }
    }

    // F1-03/F1-06: 앨범 열람 권한 - 그룹 구성원 또는 해당 어르신 본인만 허용
    public void requireViewer(String viewerId) {
        if (!isMember(viewerId) && !elderProfileId.equals(viewerId)) {
            throw new AlbumAccessDeniedException();
        }
    }

    private Optional<AlbumMember> findMember(String memberId) {
        return members.stream()
                .filter(m -> m.getMemberId().equals(memberId))
                .findFirst();
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

    // F1-06: 타임라인용 사진 목록 (인물/장소/시기 필터, 촬영·업로드 날짜 정렬 지원)
    public List<Photo> getPhotosForTimeline(String filterMemberId, String filterLocation,
                                             String filterTimePeriod, TimelineSortBy sortBy) {
        Comparator<Photo> comparator = sortBy == TimelineSortBy.UPLOADED_AT
                ? Comparator.comparing(Photo::getUploadedAt)
                : Comparator.comparing(p -> {
                    LocalDateTime shot = p.getMetadata().getShotAt();
                    return shot != null ? shot : LocalDateTime.MIN;
                });

        return photos.stream()
                .filter(p -> filterMemberId == null || p.getPersonTags().stream()
                        .anyMatch(t -> t.getMemberId().equals(filterMemberId)))
                .filter(p -> filterLocation == null || (p.getMetadata().getLocationText() != null
                        && p.getMetadata().getLocationText().contains(filterLocation)))
                .filter(p -> filterTimePeriod == null || filterTimePeriod.equals(p.getMetadata().getTimePeriod()))
                .sorted(comparator)
                .toList();
    }

    // F1-06: 타임라인 구성 최소 조건(시기 메타데이터 보유 사진 3장 이상) 충족 여부
    public boolean hasEnoughPhotosForTimeline() {
        long withTimeInfo = photos.stream()
                .filter(p -> p.getMetadata().getTimePeriod() != null || p.getMetadata().getShotAt() != null)
                .count();
        return withTimeInfo >= MIN_PHOTOS_FOR_TIMELINE;
    }

    public boolean isDuplicate(String hash) {
        return photos.stream().anyMatch(p -> p.getHash().equals(hash));
    }

    public boolean isMember(String memberId) {
        return findMember(memberId)
                .filter(m -> m.getStatus() == MembershipStatus.ACCEPTED)
                .isPresent();
    }

    public List<Photo> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    // 정식 구성원(수락 완료) ID만 반환 - 접근 제어·앨범 조회 응답에 사용
    public Set<String> getMemberIds() {
        return unmodifiableMemberIds(m -> m.getStatus() == MembershipStatus.ACCEPTED);
    }

    // 초대 수락 여부와 무관하게 모든 구성원(PENDING 포함) ID 반환 - 알림 발송 대상 산정에 사용
    public Set<String> getAllMemberIds() {
        return unmodifiableMemberIds(m -> true);
    }

    private Set<String> unmodifiableMemberIds(java.util.function.Predicate<AlbumMember> filter) {
        Set<String> ids = members.stream()
                .filter(filter)
                .map(AlbumMember::getMemberId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ids);
    }

    private Photo findPhotoOrThrow(PhotoId photoId) {
        return photos.stream()
                .filter(p -> p.getId().equals(photoId.value()))
                .findFirst()
                .orElseThrow(() -> new PhotoNotFoundException(photoId));
    }
}
