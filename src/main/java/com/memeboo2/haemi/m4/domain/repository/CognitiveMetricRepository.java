package com.memeboo2.haemi.m4.domain.repository;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveDailyMetric;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CognitiveMetricRepository {
    CognitiveDailyMetric save(CognitiveDailyMetric metric);
    Optional<CognitiveDailyMetric> findByElderIdAndMetricDate(String elderId, LocalDate metricDate);
    List<CognitiveDailyMetric> findByElderIdAndDateBetween(String elderId, LocalDate from, LocalDate to);
    List<CognitiveDailyMetric> findByAlbumIdAndDateBetween(UUID albumId, LocalDate from, LocalDate to);
    List<CognitiveDailyMetric> findByInstitutionIdAndDateBetween(String institutionId, LocalDate from, LocalDate to);
    List<String> findAllDistinctElderIds();
}
