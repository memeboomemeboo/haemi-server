package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.ElderHealth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaElderHealthRepository extends JpaRepository<ElderHealth, UUID> {
}
