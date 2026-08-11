package com.memeboo2.haemi.m4.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m4.application.dto.InstitutionElderSummary;
import com.memeboo2.haemi.m4.application.dto.ReminiscenceMetricResult;
import com.memeboo2.haemi.m4.application.service.InstitutionPortalApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "M4-InstitutionPortal", description = "F4-03 기관 담당자 회상 집계 포털")
@RestController
@RequestMapping("/api/v1/institution-portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTITUTION_ADMIN')")
public class InstitutionPortalController {

    private final InstitutionPortalApplicationService portal;
    private final InstitutionPortalRateLimiter rateLimiter;

    @GetMapping("/elders")
    public ApiResponse<List<InstitutionElderSummary>> assignedElders(@AuthenticationPrincipal AuthenticatedMember member) {
        rateLimiter.check(member.memberId());
        return ApiResponse.ok(portal.listAssigned(member.memberId()));
    }

    @GetMapping("/no-participation")
    public ApiResponse<List<InstitutionElderSummary>> noParticipation(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        rateLimiter.check(member.memberId());
        return ApiResponse.ok(portal.listNoParticipation(member.memberId(), date));
    }

    @GetMapping("/records")
    public ApiResponse<List<ReminiscenceMetricResult>> record(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam UUID elderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        rateLimiter.check(member.memberId());
        return ApiResponse.ok(portal.getRecord(member.memberId(), elderId, from, to));
    }

    @GetMapping(value = "/records/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam UUID elderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        rateLimiter.check(member.memberId());
        byte[] content = portal.exportCsv(member.memberId(), elderId, from, to).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=haemi-reminiscence.csv")
                .body(content);
    }
}
