package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDifficultyPolicyRepository extends JpaRepository<DifficultyPolicy, Integer> {
}
