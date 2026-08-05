package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.port.ElderStatusQuery;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 상태 조회 계약 구현 (#51). 존재하지 않는 어르신은 발송 불가로 안전 처리한다.
 */
@Component
@RequiredArgsConstructor
public class ElderStatusQueryAdapter implements ElderStatusQuery {

    private final ElderRepository elders;

    @Override
    @Transactional(readOnly = true)
    public ElderStatus statusOf(UUID elderId) {
        return elders.findById(elderId).map(Elder::getStatus).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDispatchable(UUID elderId) {
        return elders.findById(elderId)
                .map(elder -> elder.isDispatchable(LocalDateTime.now()))
                .orElse(false);
    }
}
