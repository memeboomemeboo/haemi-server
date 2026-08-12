package com.memeboo2.haemi.m3.domain.repository;

import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

public interface AccruedHintRepository {
    AccruedHint save(AccruedHint hint);

    // L1: 특정 사진에 적립된 최신 활성 힌트
    Optional<AccruedHint> findLatestActiveByPhoto(String elderId, UUID photoId);

    Optional<AccruedHint> findLatestReusableByPhoto(String elderId, UUID photoId, LocalDateTime servedBefore);

    // L2: 사진에 매이지 않은 어르신 일반 최신 활성 힌트
    Optional<AccruedHint> findLatestActiveGeneral(String elderId);
    Optional<AccruedHint> findLatestReusableGeneral(String elderId, LocalDateTime servedBefore);
}
