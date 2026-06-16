package com.memeboo2.haemi.m5.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m5.application.command.*;
import com.memeboo2.haemi.m5.application.dto.*;
import com.memeboo2.haemi.m5.domain.model.care.*;
import com.memeboo2.haemi.m5.domain.port.WeatherPort;
import com.memeboo2.haemi.m5.domain.repository.VoiceAlarmRepository;
import com.memeboo2.haemi.m5.domain.repository.WalkRecordRepository;
import com.memeboo2.haemi.m5.domain.repository.WalkRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareApplicationService {

    private final VoiceAlarmRepository voiceAlarmRepository;
    private final WalkRoutineRepository walkRoutineRepository;
    private final WalkRecordRepository walkRecordRepository;
    private final PhotoStoragePort storagePort;
    private final WeatherPort weatherPort;
    private final NotificationPort notificationPort;

    // F5-01: 손주 목소리 알람 생성
    @Transactional
    public VoiceAlarmResult createVoiceAlarm(CreateVoiceAlarmCommand command) {
        LocalTime from = command.alarmTime().minusMinutes(4);
        LocalTime to = command.alarmTime().plusMinutes(4);
        if (voiceAlarmRepository.existsActiveNearTime(command.elderId(), from, to)) {
            throw new AlarmTimeDuplicatedException();
        }

        String voiceKey = null;
        if (command.voiceInputStream() != null) {
            try {
                voiceKey = storagePort.store(command.voiceInputStream(),
                        command.voiceFilename() != null ? command.voiceFilename() : "voice-alarm.m4a",
                        command.voiceContentType());
            } catch (Exception e) {
                log.warn("음성 알람 저장 실패, TTS fallback 사용: elderId={}", command.elderId(), e);
            }
        }

        VoiceAlarm alarm = VoiceAlarm.create(command.elderId(), command.groupId(),
                command.alarmType(), command.alarmTime(), voiceKey, command.repeatRule());
        return VoiceAlarmResult.from(voiceAlarmRepository.save(alarm));
    }

    @Transactional(readOnly = true)
    public List<VoiceAlarmResult> getVoiceAlarms(String elderId) {
        return voiceAlarmRepository.findActiveByElderId(elderId).stream()
                .map(VoiceAlarmResult::from)
                .toList();
    }

    // F5-01: 어르신 확인 처리
    @Transactional
    public VoiceAlarmResult acknowledgeAlarm(AcknowledgeVoiceAlarmCommand command) {
        VoiceAlarm alarm = voiceAlarmRepository.findById(UUID.fromString(command.alarmId()))
                .orElseThrow(() -> new VoiceAlarmNotFoundException(command.alarmId()));
        alarm.acknowledge();
        voiceAlarmRepository.save(alarm);
        notificationPort.sendToGroup(Set.of(alarm.getGroupId()),
                "알람 확인", acknowledgementMessage(alarm.getAlarmType()));
        return VoiceAlarmResult.from(alarm);
    }

    // F5-02: 산책 루틴 생성
    @Transactional
    public WalkRoutineResult createWalkRoutine(CreateWalkRoutineCommand command) {
        WalkRoutine routine = WalkRoutine.create(command.elderId(), command.groupId(),
                command.morningTime(), command.afternoonTime(), command.targetMinutes());
        return WalkRoutineResult.from(walkRoutineRepository.save(routine));
    }

    // F5-02: 산책 시작. 악천후면 실내 운동 안내 상태로 종료.
    @Transactional
    public WalkRecordResult startWalk(StartWalkCommand command) {
        WalkRoutine routine = walkRoutineRepository.findById(UUID.fromString(command.routineId()))
                .orElseThrow(() -> new WalkRoutineNotFoundException(command.routineId()));
        WeatherCondition condition = weatherPort.currentCondition(routine.getElderId());
        WalkRecord record = WalkRecord.start(routine, condition);
        walkRecordRepository.save(record);
        if (record.getStatus() == WalkStatus.CANCELLED_BY_WEATHER) {
            notificationPort.sendToMember(routine.getElderId(),
                    "오늘은 실내 운동을 해봐요", "날씨가 좋지 않아 산책 알림을 쉬어갑니다.");
        }
        return WalkRecordResult.from(record);
    }

    // F5-02: 산책 완료
    @Transactional
    public WalkRecordResult completeWalk(CompleteWalkCommand command) {
        WalkRecord record = walkRecordRepository.findById(UUID.fromString(command.walkRecordId()))
                .orElseThrow(() -> new WalkRoutineNotFoundException(command.walkRecordId()));
        record.complete(command.durationMinutes(), command.stepCount());
        walkRecordRepository.save(record);
        notificationPort.sendToGroup(Set.of(record.getGroupId()),
                "산책 완료", "어르신이 오늘의 산책을 완료했습니다.");
        return WalkRecordResult.from(record);
    }

    @Transactional(readOnly = true)
    public WeeklyWalkSummaryResult getWeeklyWalkSummary(String elderId) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        List<WalkRecordResult> records = walkRecordRepository.findByElderIdAndDateBetween(elderId, from, to)
                .stream().map(WalkRecordResult::from).toList();
        int completedDays = (int) records.stream()
                .filter(r -> r.status() == WalkStatus.COMPLETED)
                .map(WalkRecordResult::walkDate)
                .distinct()
                .count();
        return new WeeklyWalkSummaryResult(elderId, from, to, completedDays, completedDays / 7.0, records);
    }

    private String acknowledgementMessage(AlarmType type) {
        return switch (type) {
            case MEDICATION -> "약을 드셨어요.";
            case WATER -> "물을 드셨어요.";
            case WALK -> "산책 알람을 확인하셨어요.";
            case ETC -> "알람을 확인하셨어요.";
        };
    }
}
