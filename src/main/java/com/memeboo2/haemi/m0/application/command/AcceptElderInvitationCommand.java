package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;

/**
 * F0-01-E 어르신 참여 로그인 입력. 전화번호는 해시로만 보관하며 OTP는 사용하지 않는다.
 *
 * <p>birthYear·gender·residenceType은 그룹에 어르신 프로필이 없을 때만 사용해 자동 생성한다.
 */
public record AcceptElderInvitationCommand(
        String code,
        String name,
        String phoneNumber,
        String deviceId,
        Integer birthYear,
        Gender gender,
        ResidenceType residenceType
) {
}
