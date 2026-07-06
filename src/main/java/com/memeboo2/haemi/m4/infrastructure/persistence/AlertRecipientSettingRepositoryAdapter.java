package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientSetting;
import com.memeboo2.haemi.m4.domain.repository.AlertRecipientSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AlertRecipientSettingRepositoryAdapter implements AlertRecipientSettingRepository {

    private final JpaAlertRecipientSettingRepository jpa;

    @Override
    public AlertRecipientSetting save(AlertRecipientSetting setting) {
        return jpa.save(setting);
    }

    @Override
    public Optional<AlertRecipientSetting> findByElderId(String elderId) {
        return jpa.findByElderId(elderId);
    }
}
