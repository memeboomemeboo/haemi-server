package com.memeboo2.haemi.m0.domain.event;

import java.util.UUID;

/** 가족 그룹 초대 수락 완료 이벤트. 앨범 자동 초대 등 다운스트림에서 활용한다. */
public record FamilyMemberJoinedEvent(
        UUID groupId,
        UUID memberId
) {}
