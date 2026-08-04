package com.memeboo2.haemi.m0.domain.port;

import java.util.List;
import java.util.UUID;

/** 회상 콘텐츠 생성에 필요한 최소 생애 맥락. 건강 진단 정보는 절대 포함하지 않는다. */
public interface ElderContentContextPort {
    ElderContentContext getContentContext(UUID elderId);

    record ElderContentContext(UUID elderId, UUID groupId, int personalizationLevel,
                               double completeness, List<String> lifeStoryValues,
                               List<String> sensitiveTopics) {
    }
}
