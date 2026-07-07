package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.application.command.AcceptInviteCommand;
import com.memeboo2.haemi.m1.application.command.CreateAlbumCommand;
import com.memeboo2.haemi.m1.application.command.InviteMemberCommand;
import com.memeboo2.haemi.m1.application.dto.AlbumResult;
import com.memeboo2.haemi.m1.application.dto.TimelineResult;
import com.memeboo2.haemi.m1.application.dto.PhotoResult;
import com.memeboo2.haemi.m1.application.query.GetAlbumQuery;
import com.memeboo2.haemi.m1.application.query.GetTimelineQuery;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.album.Photo;
import com.memeboo2.haemi.m1.domain.model.album.TimelineSortBy;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumApplicationService {

    private final AlbumRepository albumRepository;
    private final NotificationPort notificationPort;

    // F1-03: 앨범 생성
    @Transactional
    public AlbumResult createAlbum(CreateAlbumCommand command) {
        Album album = Album.create(command.elderProfileId(), command.groupId(), command.ownerMemberId());
        albumRepository.save(album);
        log.info("앨범 생성: albumId={}, groupId={}", album.getAlbumId(), command.groupId());
        return AlbumResult.from(album);
    }

    // F1-03: 멤버 초대 (기존 그룹 구성원만 초대 가능)
    @Transactional
    public void inviteMember(InviteMemberCommand command) {
        Album album = loadAlbumOrThrow(command.albumId());
        album.requireMember(command.inviterId());
        album.inviteMember(command.inviteeId());
        albumRepository.save(album);
        notificationPort.sendToMember(command.inviteeId(),
                "해미 앨범 초대", "가족 앨범에 초대되었습니다.");
    }

    // F1-03: 초대 수락
    @Transactional
    public void acceptInvite(AcceptInviteCommand command) {
        Album album = loadAlbumOrThrow(command.albumId());
        album.acceptInvite(command.memberId());
        albumRepository.save(album);
        log.info("초대 수락: albumId={}, memberId={}", command.albumId(), command.memberId());
    }

    // F1-03: 앨범 조회
    @Transactional(readOnly = true)
    public AlbumResult getAlbum(GetAlbumQuery query) {
        Album album = loadAlbumOrThrow(query.albumId());
        return AlbumResult.from(album);
    }

    // F1-06: 타임라인 조회
    @Transactional(readOnly = true)
    public TimelineResult getTimeline(GetTimelineQuery query) {
        Album album = loadAlbumOrThrow(query.albumId());
        TimelineSortBy sortBy = "UPLOADED_AT".equals(query.sortBy())
                ? TimelineSortBy.UPLOADED_AT : TimelineSortBy.SHOT_AT;
        List<Photo> photos = album.getPhotosForTimeline(
                query.filterMemberId(), query.filterLocation(), query.filterTimePeriod(), sortBy);

        boolean belowThreshold = !album.hasEnoughPhotosForTimeline();
        String guideMessage = belowThreshold
                ? "사진을 더 추가하면 타임라인이 만들어집니다" : null;
        boolean editable = "FAMILY".equals(query.viewerRole());

        // 연도-계절 단위로 그룹화
        Map<String, List<Photo>> grouped = photos.stream()
                .collect(Collectors.groupingBy(
                        p -> resolvePeriodLabel(p),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 날짜 미상 사진은 마지막에 배치
        List<TimelineResult.TimelineGroup> groups = new ArrayList<>();
        TimelineResult.TimelineGroup unknownGroup = null;

        for (Map.Entry<String, List<Photo>> entry : grouped.entrySet()) {
            List<PhotoResult> photoResults = entry.getValue().stream()
                    .map(PhotoResult::from).toList();
            TimelineResult.TimelineGroup group = new TimelineResult.TimelineGroup(entry.getKey(), photoResults);
            if ("날짜 미상".equals(entry.getKey())) {
                unknownGroup = group;
            } else {
                groups.add(group);
            }
        }
        if (unknownGroup != null) groups.add(unknownGroup);

        return new TimelineResult(query.albumId(), groups, photos.size(), editable, belowThreshold, guideMessage);
    }

    private String resolvePeriodLabel(Photo photo) {
        if (photo.getMetadata().getTimePeriod() != null) {
            return photo.getMetadata().getTimePeriod();
        }
        if (photo.getMetadata().getShotAt() != null) {
            int year = photo.getMetadata().getShotAt().getYear();
            int month = photo.getMetadata().getShotAt().getMonthValue();
            String season = switch (month) {
                case 3, 4, 5 -> "봄";
                case 6, 7, 8 -> "여름";
                case 9, 10, 11 -> "가을";
                default -> "겨울";
            };
            return year + "년 " + season;
        }
        return "날짜 미상";
    }

    private Album loadAlbumOrThrow(String albumId) {
        return albumRepository.findById(AlbumId.of(albumId))
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
    }
}
