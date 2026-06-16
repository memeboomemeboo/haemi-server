package com.memeboo2.haemi.m5.infrastructure.persistence;

import com.memeboo2.haemi.m5.domain.model.care.WalkRecord;
import com.memeboo2.haemi.m5.domain.repository.WalkRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalkRecordRepositoryAdapter implements WalkRecordRepository {

    private final JpaWalkRecordRepository jpa;

    @Override
    public WalkRecord save(WalkRecord record) {
        return jpa.save(record);
    }

    @Override
    public Optional<WalkRecord> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<WalkRecord> findByElderIdAndDateBetween(String elderId, LocalDate from, LocalDate to) {
        return jpa.findByElderIdAndWalkDateBetweenOrderByWalkDateAsc(elderId, from, to);
    }
}
