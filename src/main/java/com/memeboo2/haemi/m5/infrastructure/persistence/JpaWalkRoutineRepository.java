package com.memeboo2.haemi.m5.infrastructure.persistence;

import com.memeboo2.haemi.m5.domain.model.care.WalkRoutine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaWalkRoutineRepository extends JpaRepository<WalkRoutine, UUID> {
    List<WalkRoutine> findByElderIdAndActiveTrue(String elderId);
}
