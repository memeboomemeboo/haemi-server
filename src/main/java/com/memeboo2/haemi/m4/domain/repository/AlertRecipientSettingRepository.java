package com.memeboo2.haemi.m4.domain.repository;

import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientSetting;

import java.util.Optional;

public interface AlertRecipientSettingRepository {

    AlertRecipientSetting save(AlertRecipientSetting setting);

    Optional<AlertRecipientSetting> findByElderId(String elderId);
}
