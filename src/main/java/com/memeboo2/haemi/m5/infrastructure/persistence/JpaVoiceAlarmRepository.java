package com.memeboo2.haemi.m5.infrastructure.persistence;

import com.memeboo2.haemi.m5.domain.model.care.VoiceAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface JpaVoiceAlarmRepository extends JpaRepository<VoiceAlarm, UUID> {
    List<VoiceAlarm> findByElderIdAndActiveTrue(String elderId);
    List<VoiceAlarm> findByActiveTrue();
    boolean existsByElderIdAndActiveTrueAndAlarmTimeBetween(String elderId, LocalTime from, LocalTime to);
}
