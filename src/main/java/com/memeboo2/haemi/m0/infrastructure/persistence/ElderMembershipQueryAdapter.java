package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.port.ElderMembershipQuery;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 어르신 가족 그룹 멤버십 조회 구현 (#51).
 * elderId → Elder.groupId → FamilyGroup 활성 멤버십으로 확인한다.
 */
@Component
@RequiredArgsConstructor
public class ElderMembershipQueryAdapter implements ElderMembershipQuery {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveGroupMember(String elderId, UUID memberId) {
        if (elderId == null || elderId.isBlank() || memberId == null) {
            return false;
        }
        UUID eid;
        try {
            eid = UUID.fromString(elderId);
        } catch (IllegalArgumentException invalidUuid) {
            return false;
        }
        return elders.findById(eid)
                .map(Elder::getGroupId)
                .flatMap(groups::findById)
                .map(group -> group.isActiveMember(memberId))
                .orElse(false);
    }
}
