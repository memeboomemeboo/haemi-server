package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveDailyMetric;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CognitiveMetricRepositoryAdapter implements CognitiveMetricRepository {

    private final JpaCognitiveMetricRepository jpa;

    @Override
    public CognitiveDailyMetric save(CognitiveDailyMetric metric) {
        return jpa.save(metric);
    }

    @Override
    public Optional<CognitiveDailyMetric> findByElderIdAndMetricDate(String elderId, LocalDate metricDate) {
        return jpa.findByElderIdAndMetricDate(elderId, metricDate);
    }

    @Override
    public List<CognitiveDailyMetric> findByElderIdAndDateBetween(String elderId, LocalDate from, LocalDate to) {
        return jpa.findByElderIdAndMetricDateBetweenOrderByMetricDateAsc(elderId, from, to);
    }

    @Override
    public List<CognitiveDailyMetric> findByAlbumIdAndDateBetween(UUID albumId, LocalDate from, LocalDate to) {
        return jpa.findByAlbumIdAndMetricDateBetweenOrderByMetricDateAsc(albumId, from, to);
    }

    @Override
    public List<String> findAllDistinctElderIds() {
        return jpa.findDistinctElderId();
    }
}
