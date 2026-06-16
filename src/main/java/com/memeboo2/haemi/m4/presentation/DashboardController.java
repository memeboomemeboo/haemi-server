package com.memeboo2.haemi.m4.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.command.RecordCognitiveMetricCommand;
import com.memeboo2.haemi.m4.application.dto.*;
import com.memeboo2.haemi.m4.application.query.GetCognitiveMetricQuery;
import com.memeboo2.haemi.m4.application.query.GetInstitutionDashboardQuery;
import com.memeboo2.haemi.m4.application.service.DashboardApplicationService;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "M4-Dashboard", description = "F4-01 인지 리포트 / F4-02 조기 알림 / F4-03 기관 관리자 포털")
@RestController
@RequestMapping("/api/v1/cognitive-dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardApplicationService dashboardService;

    @Operation(summary = "인지 변화 일별 지표 기록 [F4-01]")
    @PostMapping("/metrics")
    public ApiResponse<CognitiveMetricResult> recordMetric(@RequestBody RecordCognitiveMetricCommand command) {
        return ApiResponse.ok(dashboardService.recordMetric(command), "지표가 기록되었습니다.");
    }

    @Operation(summary = "인지 변화 지표 조회 [F4-01]")
    @GetMapping("/metrics")
    public ApiResponse<List<CognitiveMetricResult>> getMetrics(
            @RequestParam String elderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(dashboardService.getMetrics(new GetCognitiveMetricQuery(elderId, from, to)));
    }

    @Operation(
            summary = "주간·월간 인지 리포트 생성 [F4-01]",
            description = "7일 이상 누적 데이터가 있을 때 리포트와 PDF 키를 생성합니다."
    )
    @PostMapping("/reports")
    public ApiResponse<CognitiveReportResult> generateReport(
            @RequestParam String elderId,
            @RequestParam(required = false) String albumId,
            @RequestParam(defaultValue = "WEEKLY") ReportPeriod period,
            @RequestParam(defaultValue = "IN_APP") ReportDeliveryMethod deliveryMethod) {
        return ApiResponse.ok(dashboardService.generateReport(
                new GenerateCognitiveReportCommand(elderId, albumId, period, deliveryMethod)));
    }

    @Operation(
            summary = "인지 상태 변화 조기 알림 검사 [F4-02]",
            description = "7일 미참여, 정답률 20% 이상 하락, 반응 시간 50% 이상 증가를 검사합니다."
    )
    @PostMapping("/alerts/detect")
    public ApiResponse<List<CognitiveAlertResult>> detectAlerts(@RequestParam String elderId) {
        return ApiResponse.ok(dashboardService.detectEarlyAlerts(elderId));
    }

    @Operation(summary = "기관 관리자 포털 조회 [F4-03]")
    @GetMapping("/institutions/{institutionId}")
    public ApiResponse<InstitutionDashboardResult> getInstitutionDashboard(
            @PathVariable String institutionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<String> elderIds) {
        return ApiResponse.ok(dashboardService.getInstitutionDashboard(
                new GetInstitutionDashboardQuery(institutionId, from, to, elderIds)));
    }
}
