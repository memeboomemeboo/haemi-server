package com.memeboo2.haemi.m1.domain.repository;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository {

    Album save(Album album);

    Optional<Album> findById(AlbumId id);

    Optional<Album> findByGroupId(String groupId);

    /**
     * id 오름차순 페이지. 배치가 앨범 전체를 한 번에 적재하지 않도록 이 API만 제공한다.
     * 전량 조회 메서드는 의도적으로 두지 않는다.
     */
    List<Album> findPage(int page, int size);

    /** 사진이 {@code minPhotos}장 이상인 앨범만. 사진 컬렉션을 지연 로딩하지 않으려고 조건을 쿼리로 내린다. */
    List<Album> findPageWithAtLeastPhotos(int minPhotos, int page, int size);

    /** 해당 사진이 그 가족 그룹의 앨범에 속하는지. 소유권 검증용 단건 조회. */
    boolean existsPhotoInGroup(String groupId, UUID photoId);

    /** 선다운로드 대상 식별자만. 앨범 엔티티를 적재하지 않는다. */
    List<String> findDistinctElderProfileIds();

    List<Album> findAllByElderProfileId(String elderProfileId);

    boolean existsByGroupId(String groupId);

    void delete(Album album);
}
