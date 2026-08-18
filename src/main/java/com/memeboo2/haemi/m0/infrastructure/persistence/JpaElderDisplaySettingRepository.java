package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.ElderDisplaySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaElderDisplaySettingRepository extends JpaRepository<ElderDisplaySetting, UUID> {
}
