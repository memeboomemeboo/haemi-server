package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;

import java.util.UUID;

/** 토큰 주체가 홈 화면에서 사용할 가족 그룹·어르신 프로필 연결 정보. */
public record HomeContextResult(
        UUID memberId,
        MemberRole role,
        UUID groupId,
        UUID elderId,
        ElderAccessMode accessMode,
        ElderStatus elderStatus
) {
    public static HomeContextResult from(UUID memberId, MemberRole role, Elder elder) {
        return new HomeContextResult(memberId, role, elder.getGroupId(), elder.getId(),
                elder.getAccessMode(), elder.getStatus());
    }
}
