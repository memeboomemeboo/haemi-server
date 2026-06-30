package com.memeboo2.haemi.m5.infrastructure.persistence;

import com.memeboo2.haemi.m5.domain.model.care.WalkRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaWalkRecordRepository extends JpaRepository<WalkRecord, UUID> {
    List<WalkRecord> findByElderIdAndWalkDateBetweenOrderByWalkDateAsc(String elderId, LocalDate from, LocalDate to);
}
