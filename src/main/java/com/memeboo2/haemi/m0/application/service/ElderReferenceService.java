package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.port.ElderAccessPort;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 세션·알림 모듈이 JPA 엔티티를 직접 의존하지 않도록 제공하는 읽기 모델. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ElderReferenceService implements ElderAccessPort {

    private final ElderRepository elders;

    @Override
    public ElderAccessSnapshot getRequired(UUID elderId) {
        Elder elder = elders.findById(elderId).orElseThrow(() -> new M0NotFoundException("어르신 프로필"));
        return new ElderAccessSnapshot(elder.getId(), elder.getGroupId(), elder.getStatus(), elder.getAccessMode(),
                elder.getPersonalizationLevel());
    }
}
