package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.ElderDisplaySetting;

import java.util.Optional;
import java.util.UUID;

public interface ElderDisplaySettingRepository {
    Optional<ElderDisplaySetting> findByElderId(UUID elderId);
    ElderDisplaySetting save(ElderDisplaySetting setting);
}
