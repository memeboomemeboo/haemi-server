package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m4.domain.repository.AlertRecipientSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 어르신 기기 토큰 연결 권한을 확인한다.
 * <p>Mode A는 연결된 어르신 계정만, Mode B는 해당 가족 그룹의 활성 보호자 또는 배정된 기관 담당자만
 * 기기를 연결할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ElderDeviceAccessValidator {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;
    private final MemberRepository members;
    private final AlertRecipientSettingRepository alertRecipients;

    public void requireCanBind(UUID actorId, UUID elderId) {
        Elder elder = elders.findById(elderId)
                .orElseThrow(() -> new DeviceTokenAccessDeniedException("연결할 어르신 프로필을 찾을 수 없어요."));
        if (actorId.equals(elder.getMemberId())) {
            return;
        }

        FamilyGroup group = groups.findById(elder.getGroupId())
                .orElseThrow(() -> new DeviceTokenAccessDeniedException("어르신의 가족 그룹을 찾을 수 없어요."));
        if (!group.isActiveMember(actorId)) {
            boolean isAssignedInstitutionManager = members.findById(actorId)
                    .filter(member -> member.isActive() && member.getRole() == MemberRole.INSTITUTION_ADMIN)
                    .flatMap(member -> alertRecipients.findByElderId(elderId.toString()))
                    .map(setting -> setting.getInstitutionManagerMemberIds().contains(actorId.toString()))
                    .orElse(false);
            if (!isAssignedInstitutionManager) {
                throw new DeviceTokenAccessDeniedException(
                        "어르신 기기는 연결된 어르신, 가족 구성원 또는 배정된 기관 담당자만 등록할 수 있어요.");
            }
        }
    }
}
