package com.memeboo2.haemi.m3.domain.port;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** L2 실시간 힌트를 받을 수 있도록 현재 가족 앱을 사용 중인 구성원 한 명을 찾는다. */
public interface OnlineFamilyMemberQuery {

    Optional<UUID> findOneOnlineMemberId(String elderId, LocalDateTime activeSince);
}
