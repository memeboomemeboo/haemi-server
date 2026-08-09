package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContent;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ReminiscenceContentId;
import com.memeboo2.haemi.m1.domain.repository.ReminiscenceContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ReminiscenceContentRepositoryAdapter implements ReminiscenceContentRepository {

    private final JpaReminiscenceContentRepository jpa;

    @Override
    public ReminiscenceContent save(ReminiscenceContent content) {
        return jpa.save(content);
    }

    @Override
    public Optional<ReminiscenceContent> findById(ReminiscenceContentId id) {
        return jpa.findById(id.value());
    }

    @Override
    public Optional<ReminiscenceContent> findByAlbumIdAndDate(AlbumId albumId, LocalDate date) {
        return jpa.findByAlbumIdAndGeneratedDate(albumId.value(), date);
    }

    @Override
    public Set<UUID> findRecentlyUsedPhotoIds(AlbumId albumId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return new HashSet<>(jpa.findRecentlyUsedPhotoIds(albumId.value(), since));
    }

    @Override
    public List<ReminiscenceContent> findRecentByAlbumId(AlbumId albumId, int limit) {
        return jpa.findRecentByAlbumId(albumId.value(), limit).stream().limit(limit).toList();
    }

    @Override
    public void invalidateByAlbumId(AlbumId albumId) {
        jpa.deleteAllByAlbumId(albumId.value());
    }
}
