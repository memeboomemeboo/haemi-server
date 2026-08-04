package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.SensitiveTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSensitiveTopicRepository extends JpaRepository<SensitiveTopic, UUID> {
    List<SensitiveTopic> findAllByElderId(UUID elderId);
}
