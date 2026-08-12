package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

public interface JpaAccruedHintRepository extends JpaRepository<AccruedHint, UUID> {

    Optional<AccruedHint> findFirstByElderIdAndPhotoIdAndActiveTrueOrderByCreatedAtDesc(
            String elderId, UUID photoId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT h FROM AccruedHint h
            WHERE h.elderId = :elderId AND h.photoId = :photoId AND h.active = true
              AND (h.lastServedAt IS NULL OR h.lastServedAt <= :servedBefore)
            ORDER BY h.createdAt DESC
            """)
    List<AccruedHint> findLatestReusableByPhoto(String elderId, UUID photoId, LocalDateTime servedBefore,
                                                org.springframework.data.domain.Pageable pageable);

    Optional<AccruedHint> findFirstByElderIdAndPhotoIdIsNullAndActiveTrueOrderByCreatedAtDesc(
            String elderId);
}
