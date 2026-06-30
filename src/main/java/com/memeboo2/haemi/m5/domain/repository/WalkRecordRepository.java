package com.memeboo2.haemi.m5.domain.repository;

import com.memeboo2.haemi.m5.domain.model.care.WalkRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalkRecordRepository {
    WalkRecord save(WalkRecord record);
    Optional<WalkRecord> findById(UUID id);
    List<WalkRecord> findByElderIdAndDateBetween(String elderId, LocalDate from, LocalDate to);
}
