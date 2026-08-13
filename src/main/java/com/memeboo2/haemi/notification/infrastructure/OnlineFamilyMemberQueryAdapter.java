package com.memeboo2.haemi.notification.infrastructure;

import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m3.domain.port.OnlineFamilyMemberQuery;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** 최근 하트비트가 있는 가족 앱 하나만 L2 실시간 힌트 수신자로 선정한다. */
@Component
@RequiredArgsConstructor
public class OnlineFamilyMemberQueryAdapter implements OnlineFamilyMemberQuery {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;
    private final DeviceTokenRepository deviceTokens;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findOneOnlineMemberId(String elderId, LocalDateTime activeSince) {
        UUID parsedElderId;
        try {
            parsedElderId = UUID.fromString(elderId);
        } catch (IllegalArgumentException | NullPointerException invalidId) {
            return Optional.empty();
        }
        return elders.findById(parsedElderId)
                .map(elder -> elder.getGroupId())
                .flatMap(groups::findById)
                .flatMap(group -> {
                    var memberIds = group.getActiveMembers().stream()
                            .map(member -> member.getMemberId())
                            .toList();
                    return deviceTokens.findByMemberIds(memberIds).stream()
                            .filter(token -> token.getLastUsedAt().isAfter(activeSince))
                            .max(Comparator.comparing(DeviceToken::getLastUsedAt))
                            .map(DeviceToken::getMemberId);
                });
    }
}
