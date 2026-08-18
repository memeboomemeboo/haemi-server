package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.ElderDisplaySetting;
import com.memeboo2.haemi.m0.domain.repository.ElderDisplaySettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ElderDisplaySettingRepositoryAdapter implements ElderDisplaySettingRepository {

    private final JpaElderDisplaySettingRepository jpa;

    @Override
    public Optional<ElderDisplaySetting> findByElderId(UUID elderId) {
        return jpa.findById(elderId);
    }

    @Override
    public ElderDisplaySetting save(ElderDisplaySetting setting) {
        return jpa.save(setting);
    }
}
