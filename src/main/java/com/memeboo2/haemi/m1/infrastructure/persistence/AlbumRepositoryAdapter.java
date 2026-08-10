package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AlbumRepositoryAdapter implements AlbumRepository {

    private final JpaAlbumRepository jpa;

    @Override
    public Album save(Album album) {
        return jpa.save(album);
    }

    @Override
    public Optional<Album> findById(AlbumId id) {
        return jpa.findById(id.value());
    }

    @Override
    public Optional<Album> findByGroupId(String groupId) {
        return jpa.findByGroupId(groupId);
    }

    @Override
    public List<Album> findPage(int page, int size) {
        return jpa.findAll(pageRequest(page, size)).getContent();
    }

    @Override
    public List<Album> findPageWithAtLeastPhotos(int minPhotos, int page, int size) {
        return jpa.findAllWithAtLeastPhotos(minPhotos, pageRequest(page, size)).getContent();
    }

    @Override
    public boolean existsPhotoInGroup(String groupId, UUID photoId) {
        return jpa.existsPhotoInGroup(groupId, photoId);
    }

    @Override
    public List<String> findDistinctElderProfileIds() {
        return jpa.findDistinctElderProfileIds();
    }

    // 페이지 경계가 흔들리지 않도록 항상 같은 기준으로 정렬한다.
    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public List<Album> findAllByElderProfileId(String elderProfileId) {
        return jpa.findAllByElderProfileId(elderProfileId);
    }

    @Override
    public boolean existsByGroupId(String groupId) {
        return jpa.existsByGroupId(groupId);
    }

    @Override
    public void delete(Album album) {
        jpa.delete(album);
    }
}
