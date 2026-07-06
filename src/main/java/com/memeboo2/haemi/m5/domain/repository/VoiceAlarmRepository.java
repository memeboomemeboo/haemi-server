package com.memeboo2.haemi.m5.domain.repository;

import com.memeboo2.haemi.m5.domain.model.care.VoiceAlarm;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoiceAlarmRepository {
    VoiceAlarm save(VoiceAlarm alarm);
    Optional<VoiceAlarm> findById(UUID id);
    List<VoiceAlarm> findActiveByElderId(String elderId);
    List<VoiceAlarm> findAllActive();
    boolean existsActiveNearTime(String elderId, LocalTime from, LocalTime to);
}
