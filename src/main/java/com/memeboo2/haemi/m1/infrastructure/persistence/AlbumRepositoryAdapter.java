package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public List<Album> findAll() {
        return jpa.findAll();
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
