package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.port.PhotoOwnershipPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 기존 M1 사진 저장소가 M0 인물 태깅에 제공하는 그룹 소유권 검증 어댑터. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumPhotoOwnershipAdapter implements PhotoOwnershipPort {

    private final AlbumRepository albums;

    @Override
    public void requireBelongsToGroup(UUID photoId, UUID groupId) {
        boolean belongsToGroup = albums.findAll().stream()
                .filter(album -> groupId.toString().equals(album.getGroupId()))
                .flatMap(album -> album.getPhotos().stream())
                .anyMatch(photo -> photoId.equals(photo.getId()));
        if (!belongsToGroup) {
            throw new M0NotFoundException("가족 그룹의 사진");
        }
    }
}
