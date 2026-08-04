package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.SensitiveTopic;

import java.util.List;
import java.util.UUID;

public interface SensitiveTopicRepository {
    SensitiveTopic save(SensitiveTopic topic);
    List<SensitiveTopic> findAllByElderId(UUID elderId);
}
