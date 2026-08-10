package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * 레거시 앨범의 groupId를 명세상 정식 어르신 프로필 ID로 변환한다.
 * <p>앨범의 elderProfileId는 과거에 계정 ID로도 사용됐으므로 푸시 수신자로 신뢰하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElderRecipientResolver {

    private final ElderRepository elders;

    public Optional<String> resolveByGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID canonicalGroupId = UUID.fromString(groupId);
            return elders.findByGroupId(canonicalGroupId)
                    .map(elder -> elder.getId().toString());
        } catch (IllegalArgumentException invalidUuid) {
            log.warn("어르신 기기 알림을 위한 가족 그룹 ID 형식이 올바르지 않습니다. groupId={}", groupId);
            return Optional.empty();
        }
    }
}
