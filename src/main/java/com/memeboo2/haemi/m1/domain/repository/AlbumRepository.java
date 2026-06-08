package com.memeboo2.haemi.m1.domain.repository;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository {

    Album save(Album album);

    Optional<Album> findById(AlbumId id);

    Optional<Album> findByGroupId(String groupId);

    List<Album> findAllByElderProfileId(String elderProfileId);

    boolean existsByGroupId(String groupId);

    void delete(Album album);
}
