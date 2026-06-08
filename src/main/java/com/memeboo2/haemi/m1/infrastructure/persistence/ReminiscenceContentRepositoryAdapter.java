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
        Set<UUID> ids = new HashSet<>();
        ids.addAll(jpa.findRecentlyUsedPhotoIds(albumId.value(), since));
        ids.addAll(jpa.findRecentlyUsedQuizPhotoIds(albumId.value(), since));
        return ids;
    }

    @Override
    public List<ReminiscenceContent> findRecentByAlbumId(AlbumId albumId, int limit) {
        return jpa.findRecentByAlbumId(albumId.value(), limit);
    }
}
