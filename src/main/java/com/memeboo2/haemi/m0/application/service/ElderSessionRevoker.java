package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.ElderSessionRevokeReason;
import com.memeboo2.haemi.m0.domain.repository.ElderSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 세션 폐기를 호출자의 트랜잭션과 분리해 커밋한다 (F0-01-E, EX-F005-06).
 *
 * <p>폐기가 필요한 상황은 대부분 호출자가 예외로 끝나거나(만료·상태 변경 응답) 이미 커밋된
 * AFTER_COMMIT 리스너 안이라, 같은 트랜잭션에 얹으면 폐기가 롤백되거나 flush되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ElderSessionRevoker {

    private final ElderSessionRepository sessions;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAll(UUID elderId, ElderSessionRevokeReason reason) {
        LocalDateTime now = LocalDateTime.now();
        var active = sessions.findActiveByElderId(elderId);
        active.forEach(session -> {
            session.revoke(reason, now);
            sessions.save(session);
        });
        return active.size();
    }
}
