package com.memeboo2.haemi.m4.presentation;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import com.memeboo2.haemi.m0.domain.port.ElderMembershipQuery;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.command.RecordReminiscenceMetricCommand;
import com.memeboo2.haemi.m4.application.command.UpdateAlertRecipientsCommand;
import com.memeboo2.haemi.m4.application.dto.*;
import com.memeboo2.haemi.m4.application.query.GetCognitiveMetricQuery;
import com.memeboo2.haemi.m4.application.service.DashboardApplicationService;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.presentation.dto.request.UpdateAlertRecipientsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "M4-Dashboard", description = "F4-01 회상 리포트 / F4-02 활동 변화 안내")
@RestController
@RequestMapping("/api/v1/cognitive-dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class DashboardController {

    private final DashboardApplicationService dashboardService;
    private final ElderMembershipQuery elderMemberships;

    @Operation(summary = "회상 기록 일별 집계 [F4-01]", description = "발화 감지·사진 체류·힌트 반응과 가족 기록만 집계하며 음성 내용과 정오답은 저장하지 않습니다.")
    @PostMapping("/metrics")
    public ApiResponse<ReminiscenceMetricResult> recordMetric(@AuthenticationPrincipal AuthenticatedMember member,
                                                               @RequestBody RecordReminiscenceMetricCommand command) {
        requireFamilyMember(member, command.elderId());
        return ApiResponse.ok(dashboardService.recordReminiscenceMetric(command), "회상 기록이 집계되었습니다.");
    }

    @Operation(summary = "회상 기록 일별 조회 [F4-01]")
    @GetMapping("/metrics")
    public ApiResponse<List<ReminiscenceMetricResult>> getMetrics(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam String elderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireFamilyMember(member, elderId);
        return ApiResponse.ok(dashboardService.getReminiscenceMetrics(new GetCognitiveMetricQuery(elderId, from, to)));
    }

    @Operation(
            summary = "주간·월간 회상 리포트 생성 [F4-01]",
            description = "7일 이상 누적된 회상 기록을 템플릿으로 정리합니다. 점수·정답률·예측은 포함하지 않습니다."
    )
    @PostMapping("/reports")
    public ApiResponse<ReminiscenceReportResult> generateReport(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam String elderId,
            @RequestParam(required = false) String albumId,
            @RequestParam(defaultValue = "WEEKLY") ReportPeriod period,
            @RequestParam(defaultValue = "IN_APP") ReportDeliveryMethod deliveryMethod) {
        requireFamilyMember(member, elderId);
        return ApiResponse.ok(dashboardService.generateReport(
                new GenerateCognitiveReportCommand(elderId, albumId, period, deliveryMethod)));
    }

    @Operation(summary = "회상 리포트 열람 기록 [F4-01]")
    @PostMapping("/reports/{reportId}/viewed")
    public ApiResponse<ReminiscenceReportResult> markReportViewed(@AuthenticationPrincipal AuthenticatedMember member,
                                                                   @PathVariable String reportId) {
        requireFamilyMember(member, dashboardService.getReport(reportId).getElderId());
        return ApiResponse.ok(dashboardService.markReportViewed(reportId), "리포트 열람 시각이 기록되었습니다.");
    }

    @Operation(summary = "회상 리포트 PDF 다운로드 [F4-01]")
    @GetMapping("/reports/{reportId}/pdf")
    public ResponseEntity<Resource> downloadReportPdf(@AuthenticationPrincipal AuthenticatedMember member,
                                                       @PathVariable String reportId) {
        var report = dashboardService.getReport(reportId);
        requireFamilyMember(member, report.getElderId());
        Resource resource = new FileSystemResource(report.getPdfKey());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @Operation(
            summary = "활동 변화 안내 검사 [F4-02]",
            description = "5일 미참여, 발화 감지·사진 체류·힌트 반응의 지속 변화를 확인합니다. 건강 상태를 판단하지 않습니다."
    )
    @PostMapping("/alerts/detect")
    public ApiResponse<List<CognitiveAlertResult>> detectAlerts(@AuthenticationPrincipal AuthenticatedMember member,
                                                                 @RequestParam String elderId) {
        requireFamilyMember(member, elderId);
        return ApiResponse.ok(dashboardService.detectEarlyAlerts(elderId));
    }

    @Operation(summary = "활동 변화 안내 수신자 설정 [F4-02]")
    @PutMapping("/alerts/recipients/{elderId}")
    public ApiResponse<AlertRecipientSettingResult> updateAlertRecipients(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String elderId,
            @Valid @RequestBody UpdateAlertRecipientsRequest request
    ) {
        requireFamilyMember(member, elderId);
        return ApiResponse.ok(dashboardService.updateAlertRecipients(
                new UpdateAlertRecipientsCommand(
                        elderId,
                        member.memberId().toString(),
                        request.institutionManagerMemberIds()
                )), "알림 수신자가 설정되었습니다.");
    }

    @Operation(summary = "활동 변화 안내 수신자 조회 [F4-02]")
    @GetMapping("/alerts/recipients/{elderId}")
    public ApiResponse<AlertRecipientSettingResult> getAlertRecipients(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String elderId
    ) {
        if (member.role() == MemberRole.FAMILY) {
            requireFamilyMember(member, elderId);
            return ApiResponse.ok(dashboardService.getAlertRecipients(elderId));
        }
        AlertRecipientSettingResult result = dashboardService.getAlertRecipients(elderId);
        if (member.role() != MemberRole.INSTITUTION_ADMIN
                || !result.institutionManagerMemberIds().contains(member.memberId().toString())) {
            throw new M0AccessDeniedException("알림 수신자로 등록된 기관 담당자만 설정을 조회할 수 있어요.");
        }
        return ApiResponse.ok(result);
    }

    private void requireFamilyMember(AuthenticatedMember member, String elderId) {
        if (member.role() != MemberRole.FAMILY
                || !elderMemberships.isActiveGroupMember(elderId, member.memberId())) {
            throw new M0AccessDeniedException("해당 어르신의 가족 그룹 구성원만 이용할 수 있어요.");
        }
    }
}
