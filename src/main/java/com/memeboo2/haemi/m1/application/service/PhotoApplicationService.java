package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.application.command.RemovePhotoCommand;
import com.memeboo2.haemi.m1.application.command.SavePhotoCommand;
import com.memeboo2.haemi.m1.application.command.SyncPhotosCommand;
import com.memeboo2.haemi.m1.application.command.UpdatePhotoMemoCommand;
import com.memeboo2.haemi.m1.application.dto.PhotoResult;
import com.memeboo2.haemi.m1.application.dto.SyncHistoryResult;
import com.memeboo2.haemi.m1.application.query.GetSyncHistoryQuery;
import com.memeboo2.haemi.m1.domain.model.album.*;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.PhotoSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoApplicationService {

    private final AlbumRepository albumRepository;
    private final PhotoStoragePort photoStoragePort;
    private final NotificationPort notificationPort;
    private final PhotoSyncLogRepository photoSyncLogRepository;

    @Value("${haemi.album.sync-low-battery-threshold:20}")
    private int lowBatteryThreshold = 20;

    // F1-01: 사진 개별 저장
    @Transactional
    public PhotoResult savePhoto(SavePhotoCommand command) {
        Album album = loadAlbumOrThrow(command.albumId());
        album.requireMember(command.uploadedBy());

        // 중복 검사는 Album 애그리게이트에서 수행
        String storageKey;
        try {
            storageKey = photoStoragePort.store(
                    command.inputStream(),
                    command.originalFilename(),
                    command.contentType()
            );
        } catch (Exception e) {
            throw new PhotoStorageException("사진 저장에 실패했습니다.", e);
        }

        Photo photo = buildAndAddPhoto(album, storageKey, command.originalFilename(),
                command.contentType(), command.fileSize(), command.shotAt(),
                command.latitude(), command.longitude(), command.sha256Hash(), command.uploadedBy());
        albumRepository.save(album);

        // 그룹 전체(초대 수락 여부와 무관하게 모든 구성원)에 알림
        notificationPort.sendToGroup(album.getAllMemberIds(),
                "새 사진 추가됨",
                command.uploadedBy() + "님이 사진을 추가했습니다.");

        log.info("사진 저장 완료: photoId={}, albumId={}", photo.getPhotoId(), command.albumId());
        return PhotoResult.from(photo);
    }

    // F1-02: 일괄 동기화 (증분 - 중복 사진은 건너뜀, Wi-Fi/배터리 조건 검증 및 이력 기록)
    @Transactional
    public SyncResult syncPhotos(SyncPhotosCommand command) {
        Album album = loadAlbumOrThrow(command.albumId());
        album.requireMember(command.uploadedBy());

        if (command.wifiOnly() && command.networkType() == NetworkType.CELLULAR) {
            throw SyncConditionNotMetException.wifiRequired();
        }
        if (command.batteryLevel() != null && command.batteryLevel() <= lowBatteryThreshold) {
            throw SyncConditionNotMetException.lowBattery();
        }

        List<PhotoResult> saved = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (SavePhotoCommand photoCmd : command.photos()) {
            if (album.isDuplicate(photoCmd.sha256Hash())) {
                skipped.add(photoCmd.originalFilename());
                continue;
            }
            try {
                String storageKey = photoStoragePort.store(
                        photoCmd.inputStream(),
                        photoCmd.originalFilename(),
                        photoCmd.contentType()
                );
                Photo photo = buildAndAddPhoto(album, storageKey, photoCmd.originalFilename(),
                        photoCmd.contentType(), photoCmd.fileSize(), photoCmd.shotAt(),
                        photoCmd.latitude(), photoCmd.longitude(), photoCmd.sha256Hash(), command.uploadedBy());
                saved.add(PhotoResult.from(photo));
            } catch (Exception e) {
                log.warn("동기화 중 사진 저장 실패: {}", photoCmd.originalFilename(), e);
                skipped.add(photoCmd.originalFilename());
            }
        }

        albumRepository.save(album);

        photoSyncLogRepository.save(PhotoSyncLog.create(
                album.getAlbumId(), command.uploadedBy(),
                command.photos().size(), saved.size(), skipped.size(),
                command.networkType(), command.batteryLevel(), command.backgroundSync()));

        notificationPort.sendToMember(command.uploadedBy(),
                "동기화 완료",
                "총 " + saved.size() + "장 동기화 완료");

        log.info("사진 동기화 완료: albumId={}, saved={}, skipped={}",
                command.albumId(), saved.size(), skipped.size());
        return new SyncResult(saved, skipped);
    }

    // F1-02: 날짜별 동기화 이력 조회
    @Transactional(readOnly = true)
    public List<SyncHistoryResult> getSyncHistory(GetSyncHistoryQuery query) {
        Album album = loadAlbumOrThrow(query.albumId());
        album.requireMember(query.viewerMemberId());
        return photoSyncLogRepository.findTop30ByAlbumIdOrderBySyncedAtDesc(album.getAlbumId()).stream()
                .map(SyncHistoryResult::from)
                .toList();
    }

    // F1-04: 사진 메모 & 회상 태깅
    @Transactional
    public PhotoResult updatePhotoMemo(UpdatePhotoMemoCommand command) {
        Album album = loadAlbumOrThrow(command.albumId());
        album.requireMember(command.requestingMemberId());
        PhotoId photoId = PhotoId.of(command.photoId());

        album.updatePhotoMemo(photoId, command.timePeriod(), command.locationText(), command.memo());

        List<PersonTag> tags = command.personTags() == null ? List.of() :
                command.personTags().stream()
                        .map(t -> PersonTag.of(t.memberId(), t.memberName()))
                        .toList();
        if (!tags.isEmpty()) {
            album.tagPersonsOnPhoto(photoId, tags);
        }

        albumRepository.save(album);

        Photo updatedPhoto = album.getPhotos().stream()
                .filter(p -> p.getId().toString().equals(command.photoId()))
                .findFirst()
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        log.info("메모 저장 완료: photoId={}", command.photoId());
        return PhotoResult.from(updatedPhoto);
    }

    // 사진 삭제
    @Transactional
    public void removePhoto(RemovePhotoCommand command) {
        Album album = loadAlbumOrThrow(command.albumId());
        album.removePhoto(PhotoId.of(command.photoId()), command.requestingMemberId());
        albumRepository.save(album);
    }

    private Photo buildAndAddPhoto(Album album, String storageKey, String originalFilename, String contentType,
                                    long fileSize, java.time.LocalDateTime shotAt, Double latitude, Double longitude,
                                    String sha256Hash, String uploadedBy) {
        PhotoFile file = PhotoFile.of(storageKey, originalFilename, contentType, fileSize);
        PhotoMetadata metadata = PhotoMetadata.of(shotAt, latitude, longitude);
        return album.addPhoto(file, metadata, sha256Hash, uploadedBy);
    }

    private Album loadAlbumOrThrow(String albumId) {
        return albumRepository.findById(AlbumId.of(albumId))
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
    }

    public record SyncResult(List<PhotoResult> saved, List<String> skipped) {}
}
