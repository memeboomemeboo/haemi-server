package com.memeboo2.haemi.m5.infrastructure.persistence;

import com.memeboo2.haemi.m5.domain.model.care.VoiceAlarm;
import com.memeboo2.haemi.m5.domain.repository.VoiceAlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VoiceAlarmRepositoryAdapter implements VoiceAlarmRepository {

    private final JpaVoiceAlarmRepository jpa;

    @Override
    public VoiceAlarm save(VoiceAlarm alarm) {
        return jpa.save(alarm);
    }

    @Override
    public Optional<VoiceAlarm> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<VoiceAlarm> findActiveByElderId(String elderId) {
        return jpa.findByElderIdAndActiveTrue(elderId);
    }

    @Override
    public boolean existsActiveNearTime(String elderId, LocalTime from, LocalTime to) {
        return jpa.existsByElderIdAndActiveTrueAndAlarmTimeBetween(elderId, from, to);
    }
}
