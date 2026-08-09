package com.memeboo2.haemi.m0.domain.port;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;

import java.util.UUID;

/** B의 세션·알림 라인이 사용하는 M0 어르신 계약. */
public interface ElderAccessPort {
    ElderAccessSnapshot getRequired(UUID elderId);

    record ElderAccessSnapshot(UUID elderId, UUID groupId, ElderStatus status,
                               ElderAccessMode accessMode, int personalizationLevel) {
        public boolean isElderFacingDeliveryAllowed() {
            return status == ElderStatus.ACTIVE || status == ElderStatus.DECLINING;
        }
    }
}
