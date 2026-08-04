package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.SensitiveTopic;

import java.util.UUID;

public record SensitiveTopicResult(UUID topicId, String keyword, String reason) {
    public static SensitiveTopicResult from(SensitiveTopic topic) {
        return new SensitiveTopicResult(topic.getId(), topic.getKeyword(), topic.getReason());
    }
}
