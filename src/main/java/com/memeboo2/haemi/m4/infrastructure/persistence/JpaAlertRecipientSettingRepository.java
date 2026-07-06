package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaAlertRecipientSettingRepository
        extends JpaRepository<AlertRecipientSetting, UUID> {

    Optional<AlertRecipientSetting> findByElderId(String elderId);
}
