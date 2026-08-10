package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.Photo;
import com.memeboo2.haemi.m1.domain.model.album.PhotoFile;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 페이징·필터 쿼리는 목으로 검증할 수 없다. 실제 DB로 확인한다 (#85).
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@ActiveProfiles("test")
@Transactional
class AlbumRepositoryAdapterIntegrationTest {

    private final AlbumRepository albums;

    @Autowired
    AlbumRepositoryAdapterIntegrationTest(AlbumRepository albums) {
        this.albums = albums;
    }

    @Test
    @DisplayName("페이지 크기를 넘겨도 요청한 만큼만 돌려주고, 범위를 넘은 페이지는 비어 있다")
    void findPageReturnsBoundedResults() {
        for (int index = 0; index < 5; index++) {
            albums.save(album());
        }

        List<Album> firstPage = albums.findPage(0, 2);
        List<Album> farPage = albums.findPage(500, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(farPage).isEmpty();
    }

    @Test
    @DisplayName("사진 수 조건이 쿼리에서 걸러진다")
    void findPageWithAtLeastPhotosFiltersByPhotoCount() {
        Album enough = albums.save(albumWithPhotos(5));
        albums.save(albumWithPhotos(4));

        List<Album> matched = albums.findPageWithAtLeastPhotos(5, 0, 100);

        assertThat(matched).extracting(Album::getId).contains(enough.getId());
        assertThat(matched).allSatisfy(album -> assertThat(album.getPhotos()).hasSizeGreaterThanOrEqualTo(5));
    }

    @Test
    @DisplayName("사진 소유권은 같은 가족 그룹의 앨범일 때만 참이다")
    void existsPhotoInGroupOnlyMatchesOwningGroup() {
        Album owning = albumWithPhotos(1);
        albums.save(owning);
        Photo photo = owning.getPhotos().getFirst();

        assertThat(albums.existsPhotoInGroup(owning.getGroupId(), photo.getId())).isTrue();
        assertThat(albums.existsPhotoInGroup(UUID.randomUUID().toString(), photo.getId())).isFalse();
        assertThat(albums.existsPhotoInGroup(owning.getGroupId(), UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("선다운로드 대상 ID는 중복 없이 모인다")
    void findDistinctElderProfileIdsDeduplicates() {
        String elderProfileId = UUID.randomUUID().toString();
        albums.save(Album.create(elderProfileId, UUID.randomUUID().toString(), "owner"));
        albums.save(Album.create(elderProfileId, UUID.randomUUID().toString(), "owner"));

        List<String> ids = albums.findDistinctElderProfileIds();

        assertThat(ids).filteredOn(elderProfileId::equals).hasSize(1);
    }

    private static Album album() {
        return Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "owner");
    }

    private static Album albumWithPhotos(int count) {
        Album album = album();
        for (int index = 0; index < count; index++) {
            album.addPhoto(
                    PhotoFile.of("key-" + UUID.randomUUID(), "photo.jpg", "image/jpeg", 1024),
                    PhotoMetadata.of(LocalDateTime.now(), null, null),
                    "hash-" + UUID.randomUUID(),
                    "owner");
        }
        return album;
    }
}
