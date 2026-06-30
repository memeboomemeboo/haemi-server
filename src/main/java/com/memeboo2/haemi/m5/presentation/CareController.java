package com.memeboo2.haemi.m5.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m5.application.command.*;
import com.memeboo2.haemi.m5.application.dto.*;
import com.memeboo2.haemi.m5.application.service.CareApplicationService;
import com.memeboo2.haemi.m5.presentation.dto.request.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "M5-Care", description = "F5-01 손주 목소리 알람 / F5-02 하루 10분 산책 유도")
@RestController
@RequestMapping("/api/v1/care")
@RequiredArgsConstructor
public class CareController {

    private final CareApplicationService careService;

    @Operation(
            summary = "손주 목소리 알람 생성 [F5-01]",
            description = "음성 파일이 없거나 저장에 실패하면 기본 TTS 목소리로 대체합니다."
    )
    @PostMapping(value = "/voice-alarms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VoiceAlarmResult> createVoiceAlarm(
            @RequestPart("data") @Valid CreateVoiceAlarmRequest request,
            @Parameter(description = "가족 음성 녹음 파일, 최대 30초")
            @RequestPart(value = "voice", required = false) MultipartFile voice) throws IOException {
        VoiceAlarmResult result = careService.createVoiceAlarm(new CreateVoiceAlarmCommand(
                request.elderId(), request.groupId(), request.alarmType(),
                request.alarmTime(), request.repeatRule(),
                voice != null ? voice.getInputStream() : null,
                voice != null ? voice.getOriginalFilename() : null,
                voice != null ? voice.getContentType() : null
        ));
        return ApiResponse.ok(result, "알람이 설정되었습니다.");
    }

    @Operation(summary = "손주 목소리 알람 목록 조회 [F5-01]")
    @GetMapping("/voice-alarms")
    public ApiResponse<List<VoiceAlarmResult>> getVoiceAlarms(@RequestParam String elderId) {
        return ApiResponse.ok(careService.getVoiceAlarms(elderId));
    }

    @Operation(summary = "알람 확인 처리 [F5-01]")
    @PostMapping("/voice-alarms/{alarmId}/acknowledge")
    public ApiResponse<VoiceAlarmResult> acknowledgeAlarm(
            @PathVariable String alarmId,
            @RequestParam String elderId) {
        return ApiResponse.ok(careService.acknowledgeAlarm(
                new AcknowledgeVoiceAlarmCommand(alarmId, elderId)), "확인되었습니다.");
    }

    @Operation(summary = "산책 루틴 설정 [F5-02]")
    @PostMapping("/walk-routines")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WalkRoutineResult> createWalkRoutine(
            @Valid @RequestBody CreateWalkRoutineRequest request) {
        return ApiResponse.ok(careService.createWalkRoutine(new CreateWalkRoutineCommand(
                request.elderId(), request.groupId(), request.morningTime(),
                request.afternoonTime(), request.targetMinutes())), "산책 루틴이 설정되었습니다.");
    }

    @Operation(summary = "산책 시작 [F5-02]")
    @PostMapping("/walk-routines/{routineId}/start")
    public ApiResponse<WalkRecordResult> startWalk(@PathVariable String routineId) {
        return ApiResponse.ok(careService.startWalk(new StartWalkCommand(routineId)));
    }

    @Operation(summary = "산책 완료 [F5-02]")
    @PostMapping("/walk-records/{walkRecordId}/complete")
    public ApiResponse<WalkRecordResult> completeWalk(
            @PathVariable String walkRecordId,
            @Valid @RequestBody CompleteWalkRequest request) {
        return ApiResponse.ok(careService.completeWalk(new CompleteWalkCommand(
                walkRecordId, request.durationMinutes(), request.stepCount())), "잘 하셨어요!");
    }

    @Operation(summary = "주간 산책 달성률 조회 [F5-02]")
    @GetMapping("/walk-records/weekly-summary")
    public ApiResponse<WeeklyWalkSummaryResult> getWeeklySummary(@RequestParam String elderId) {
        return ApiResponse.ok(careService.getWeeklyWalkSummary(elderId));
    }
}
