package com.memeboo2.haemi.auth.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * 회원이 속한 가족 그룹 식별자를 조회한다.
 * auth 모듈이 m0 모듈에 직접 의존하지 않도록 포트로 분리하고, 구현은 m0에서 제공한다.
 */
public interface MemberGroupQueryPort {
    Optional<UUID> findGroupIdByMemberId(UUID memberId);
}
