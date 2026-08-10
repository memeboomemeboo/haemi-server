package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.port.ElderDeviceIdentityQuery;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ElderDeviceIdentityQueryAdapter implements ElderDeviceIdentityQuery {

    private final ElderRepository elders;

    @Override
    @Transactional(readOnly = true)
    public boolean isLinkedElderMember(String elderId, UUID memberId) {
        if (elderId == null || memberId == null) {
            return false;
        }
        try {
            return elders.findById(UUID.fromString(elderId))
                    .map(Elder::getMemberId)
                    .filter(memberId::equals)
                    .isPresent();
        } catch (IllegalArgumentException invalidId) {
            return false;
        }
    }
}
