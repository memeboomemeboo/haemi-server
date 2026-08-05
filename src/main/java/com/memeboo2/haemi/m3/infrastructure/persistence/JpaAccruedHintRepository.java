package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaAccruedHintRepository extends JpaRepository<AccruedHint, UUID> {

    Optional<AccruedHint> findFirstByElderIdAndPhotoIdAndActiveTrueOrderByCreatedAtDesc(
            String elderId, UUID photoId);

    Optional<AccruedHint> findFirstByElderIdAndPhotoIdIsNullAndActiveTrueOrderByCreatedAtDesc(
            String elderId);
}
