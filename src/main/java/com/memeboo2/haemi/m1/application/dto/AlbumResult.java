package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.album.Album;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AlbumResult(
        String albumId,
        String elderProfileId,
        String groupId,
        String ownerMemberId,
        Set<String> memberIds,
        int photoCount,
        List<PhotoResult> recentPhotos,
        LocalDateTime createdAt
) {
    public static AlbumResult from(Album album) {
        List<PhotoResult> recent = album.getPhotos().stream()
                .limit(10)
                .map(PhotoResult::from)
                .toList();
        return new AlbumResult(
                album.getId().toString(),
                album.getElderProfileId(),
                album.getGroupId(),
                album.getOwnerMemberId(),
                album.getMemberIds(),
                album.getPhotos().size(),
                recent,
                album.getCreatedAt()
        );
    }
}
