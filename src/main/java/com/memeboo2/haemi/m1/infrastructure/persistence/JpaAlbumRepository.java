package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAlbumRepository extends JpaRepository<Album, UUID> {

    Optional<Album> findByGroupId(String groupId);

    List<Album> findAllByElderProfileId(String elderProfileId);

    boolean existsByGroupId(String groupId);

    @Query("""
            SELECT a FROM Album a
            WHERE a.ownerMemberId = :memberId
               OR EXISTS (SELECT 1 FROM a.members m WHERE m.memberId = :memberId)
            """)
    List<Album> findAllByMemberId(String memberId);
}
