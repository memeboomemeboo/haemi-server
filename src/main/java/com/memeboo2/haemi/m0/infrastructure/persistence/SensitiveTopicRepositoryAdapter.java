package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.SensitiveTopic;
import com.memeboo2.haemi.m0.domain.repository.SensitiveTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SensitiveTopicRepositoryAdapter implements SensitiveTopicRepository {

    private final JpaSensitiveTopicRepository sensitiveTopics;

    @Override
    public SensitiveTopic save(SensitiveTopic topic) {
        return sensitiveTopics.save(topic);
    }

    @Override
    public List<SensitiveTopic> findAllByElderId(UUID elderId) {
        return sensitiveTopics.findAllByElderId(elderId);
    }
}
