package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import com.memeboo2.haemi.m3.domain.repository.AccruedHintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccruedHintRepositoryAdapter implements AccruedHintRepository {

    private final JpaAccruedHintRepository jpa;

    @Override
    public AccruedHint save(AccruedHint hint) {
        return jpa.save(hint);
    }

    @Override
    public Optional<AccruedHint> findLatestActiveByPhoto(String elderId, UUID photoId) {
        return jpa.findFirstByElderIdAndPhotoIdAndActiveTrueOrderByCreatedAtDesc(elderId, photoId);
    }

    @Override
    public Optional<AccruedHint> findLatestReusableByPhoto(String elderId, UUID photoId, LocalDateTime servedBefore) {
        return jpa.findLatestReusableByPhoto(elderId, photoId, servedBefore,
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst();
    }

    @Override
    public Optional<AccruedHint> findLatestActiveGeneral(String elderId) {
        return jpa.findFirstByElderIdAndPhotoIdIsNullAndActiveTrueOrderByCreatedAtDesc(elderId);
    }

    @Override
    public Optional<AccruedHint> findLatestReusableGeneral(String elderId, LocalDateTime servedBefore) {
        return jpa.findLatestReusableGeneral(elderId, servedBefore, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst();
    }
}
