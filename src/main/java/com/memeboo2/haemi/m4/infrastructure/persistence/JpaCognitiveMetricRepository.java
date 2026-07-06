package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveDailyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCognitiveMetricRepository extends JpaRepository<CognitiveDailyMetric, UUID> {
    Optional<CognitiveDailyMetric> findByElderIdAndMetricDate(String elderId, LocalDate metricDate);
    List<CognitiveDailyMetric> findByElderIdAndMetricDateBetweenOrderByMetricDateAsc(String elderId, LocalDate from, LocalDate to);
    List<CognitiveDailyMetric> findByAlbumIdAndMetricDateBetweenOrderByMetricDateAsc(UUID albumId, LocalDate from, LocalDate to);
    List<CognitiveDailyMetric> findByInstitutionIdAndMetricDateBetweenOrderByMetricDateAsc(String institutionId, LocalDate from, LocalDate to);
    @Query("SELECT DISTINCT m.elderId FROM CognitiveDailyMetric m")
    List<String> findDistinctElderId();
}
