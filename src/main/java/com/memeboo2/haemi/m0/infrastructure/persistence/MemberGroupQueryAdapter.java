package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.auth.domain.port.MemberGroupQueryPort;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 회원 → 가족 그룹 조회 구현.
 * 가족은 활성 멤버십으로, 어르신은 연결된 프로필의 groupId로 찾는다.
 * 기관 관리자처럼 소속 그룹이 없으면 비어 있는 값을 돌려준다.
 */
@Component
@RequiredArgsConstructor
public class MemberGroupQueryAdapter implements MemberGroupQueryPort {

    private final FamilyGroupRepository groups;
    private final ElderRepository elders;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findGroupIdByMemberId(UUID memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return groups.findActiveByMemberId(memberId)
                .map(FamilyGroup::getId)
                .or(() -> elders.findByMemberId(memberId).map(Elder::getGroupId));
    }
}
