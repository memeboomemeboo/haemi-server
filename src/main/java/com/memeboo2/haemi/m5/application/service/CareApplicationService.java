package com.memeboo2.haemi.m5.application.service;

import com.memeboo2.haemi.common.support.DomainIds;

import com.memeboo2.haemi.m0.domain.port.ElderStatusQuery;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
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
import java.time.LocalDateTime;
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
    private final AlbumRepository albumRepository;
    private final ElderStatusQuery elderStatusQuery;

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

    // F5-01: 로테이션용 가족 음성 추가
    @Transactional
    public VoiceAlarmResult addAlarmVoice(AddAlarmVoiceCommand command) {
        VoiceAlarm alarm = voiceAlarmRepository.findById(DomainIds.parseUuid(command.alarmId(), "음성 알람 ID"))
                .orElseThrow(() -> new VoiceAlarmNotFoundException(command.alarmId()));
        if (!alarm.getElderId().equals(command.elderId())) {
            throw new AlarmAccessDeniedException();
        }
        if (command.voiceInputStream() == null) {
            throw new EmptyAlarmVoiceException();
        }
        String voiceKey;
        try {
            voiceKey = storagePort.store(command.voiceInputStream(),
                    command.voiceFilename() != null ? command.voiceFilename() : "voice-alarm.m4a",
                    command.voiceContentType());
        } catch (Exception e) {
            throw new IllegalStateException("음성 저장에 실패했습니다.", e);
        }
        alarm.addVoice(voiceKey);
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
        VoiceAlarm alarm = voiceAlarmRepository.findById(DomainIds.parseUuid(command.alarmId(), "음성 알람 ID"))
                .orElseThrow(() -> new VoiceAlarmNotFoundException(command.alarmId()));
        if (!alarm.getElderId().equals(command.elderId())) {
            throw new AlarmAccessDeniedException();
        }
        alarm.acknowledge();
        voiceAlarmRepository.save(alarm);
        notificationPort.sendToGroup(familyMemberIds(alarm.getGroupId()),
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
        WalkRoutine routine = walkRoutineRepository.findById(DomainIds.parseUuid(command.routineId(), "산책 루틴 ID"))
                .orElseThrow(() -> new WalkRoutineNotFoundException(command.routineId()));
        WeatherCondition condition = weatherPort.currentCondition(routine.getElderId());
        WalkRecord record = WalkRecord.start(routine, condition);
        walkRecordRepository.save(record);
        if (record.getStatus() == WalkStatus.CANCELLED_BY_WEATHER) {
            notificationPort.sendToElder(routine.getElderId(),
                    "오늘은 실내 운동을 해봐요", "날씨가 좋지 않아 산책 알림을 쉬어갑니다.");
        }
        return WalkRecordResult.from(record);
    }

    // F5-02: 산책 완료
    @Transactional
    public WalkRecordResult completeWalk(CompleteWalkCommand command) {
        WalkRecord record = walkRecordRepository.findById(DomainIds.parseUuid(command.walkRecordId(), "산책 기록 ID"))
                .orElseThrow(() -> new WalkRoutineNotFoundException(command.walkRecordId()));
        record.complete(command.durationMinutes(), command.stepCount());
        walkRecordRepository.save(record);
        notificationPort.sendToGroup(familyMemberIds(record.getGroupId()),
                "산책 완료", "어르신이 오늘의 산책을 완료했습니다.");
        return WalkRecordResult.from(record);
    }

    @Transactional
    public void processDueReminders(LocalDateTime now) {
        voiceAlarmRepository.findAllActive().forEach(alarm -> processVoiceAlarm(alarm, now));
        // F5-02 산책 알림은 보류(#47): 스케줄러에서 산책 루틴을 처리하지 않는다.
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
            case REMINISCENCE -> "회상 알람을 확인하셨어요.";
            case MEDICATION -> "약을 드셨어요.";
            case WATER -> "물을 드셨어요.";
            case WALK -> "산책 알람을 확인하셨어요.";
            case ETC -> "알람을 확인하셨어요.";
        };
    }

    private void processVoiceAlarm(VoiceAlarm alarm, LocalDateTime now) {
        if (alarm.shouldTrigger(now)) {
            // 발송 직전 어르신 상태 검증 (EX-F501-06): 사별/입원/무음기간이면 발송 생략
            if (!elderStatusQuery.isDispatchable(alarm.getElderId())) {
                log.info("어르신 상태 검증 실패로 알람 발송 생략: alarmId={}, elderId={}",
                        alarm.getId(), alarm.getElderId());
                return;
            }
            // 알람 자체의 발송 가능 여부 검증 (작고 가족 음성 등)
            if (!alarm.isDispatchable()) {
                log.info("발송 직전 알람 상태 검증 실패로 발송 생략: alarmId={}", alarm.getId());
                return;
            }
            alarm.markTriggered(now);
            notificationPort.sendToElder(alarm.getElderId(),
                    alarmTitle(alarm.getAlarmType()),
                    alarm.usesTtsFallback()
                            ? "가족이 설정한 알람이에요. 화면의 안내 문구를 읽어드릴게요."
                            : "가족의 목소리 알람이 도착했어요.",
                    java.util.Map.of(
                            "type", "VOICE_ALARM",
                            "alarmId", alarm.getId().toString(),
                            "alarmType", alarm.getAlarmType().name(),
                            "ttsFallback", Boolean.toString(alarm.usesTtsFallback()),
                            "voiceKey", alarm.getVoiceKey() == null ? "" : alarm.getVoiceKey()
                    ));
            alarm.rotateVoice(); // 다음 발송을 위해 로테이션
            voiceAlarmRepository.save(alarm);
        }

        if (alarm.isNoResponseDue(now)) {
            alarm.markNoResponseNotified(now);
            voiceAlarmRepository.save(alarm);
            notificationPort.sendToGroup(familyMemberIds(alarm.getGroupId()),
                    "알람 무응답",
                    "어르신이 알람을 10분 동안 확인하지 않았습니다.");
        }
    }

    private Set<String> familyMemberIds(String groupId) {
        return albumRepository.findByGroupId(groupId)
                .map(album -> album.getMemberIds())
                .filter(memberIds -> !memberIds.isEmpty())
                .orElseGet(() -> {
                    log.warn("가족 알림 대상 없음: groupId={}", groupId);
                    return Set.of();
                });
    }

    private String alarmTitle(AlarmType type) {
        return switch (type) {
            case REMINISCENCE -> "가족의 회상 목소리 알람이에요";
            case MEDICATION -> "약 드실 시간이에요";
            case WATER -> "물 드실 시간이에요";
            case WALK -> "산책할 시간이에요";
            case ETC -> "가족 알람이 도착했어요";
        };
    }
}
