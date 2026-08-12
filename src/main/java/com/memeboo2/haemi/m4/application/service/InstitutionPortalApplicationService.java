package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.InstitutionAssignmentRepository;
import com.memeboo2.haemi.m4.application.dto.InstitutionElderSummary;
import com.memeboo2.haemi.m4.application.dto.ReminiscenceMetricResult;
import com.memeboo2.haemi.m4.domain.model.dashboard.InstitutionAuditAction;
import com.memeboo2.haemi.m4.domain.model.dashboard.InstitutionPortalAuditLog;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import com.memeboo2.haemi.m4.domain.repository.InstitutionPortalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** F4-03 기관 담당자 포털. 배정·2FA·12개월 조회 제한을 한 경로에서 강제한다. */
@Service
@RequiredArgsConstructor
public class InstitutionPortalApplicationService {

    private static final int MAX_LOOKBACK_MONTHS = 12;

    private final MemberRepository members;
    private final InstitutionAssignmentRepository assignments;
    private final ElderRepository elders;
    private final CognitiveMetricRepository metrics;
    private final InstitutionPortalAuditLogRepository auditLogs;

    @Transactional
    public List<InstitutionElderSummary> listAssigned(UUID memberId) {
        requireOperator(memberId, null, InstitutionAuditAction.ASSIGNED_ELDER_LIST);
        return assignments.findAllByInstitutionAdminMemberIdAndActiveTrue(memberId).stream()
                .map(assignment -> elders.findById(assignment.getElderId()).orElse(null))
                .filter(elder -> elder != null && elder.getStatus() != ElderStatus.MEMORIAL)
                .map(elder -> InstitutionElderSummary.from(elder, lastParticipationDate(elder)))
                .toList();
    }

    @Transactional
    public List<InstitutionElderSummary> listNoParticipation(UUID memberId, LocalDate date) {
        requireOperator(memberId, null, InstitutionAuditAction.NO_PARTICIPATION_LIST);
        return assignments.findAllByInstitutionAdminMemberIdAndActiveTrue(memberId).stream()
                .map(assignment -> elders.findById(assignment.getElderId()).orElse(null))
                .filter(elder -> elder != null && elder.getStatus() != ElderStatus.MEMORIAL)
                .filter(elder -> metrics.findByElderIdAndMetricDate(elder.getId().toString(), date)
                        .map(metric -> !metric.participated()).orElse(true))
                .map(elder -> InstitutionElderSummary.from(elder, lastParticipationDate(elder)))
                .toList();
    }

    @Transactional
    public List<ReminiscenceMetricResult> getRecord(UUID memberId, UUID elderId,
                                                     LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        requireAssigned(memberId, elderId, InstitutionAuditAction.ELDER_RECORD_VIEW);
        return metrics.findByElderIdAndDateBetween(elderId.toString(), from, to).stream()
                .map(ReminiscenceMetricResult::from)
                .toList();
    }

    @Transactional
    public String exportCsv(UUID memberId, UUID elderId, LocalDate from, LocalDate to) {
        List<ReminiscenceMetricResult> records = getRecord(memberId, elderId, from, to);
        auditLogs.save(InstitutionPortalAuditLog.record(memberId, elderId, InstitutionAuditAction.CSV_EXPORT, true));
        StringBuilder csv = new StringBuilder("metricDate,sessionCount,voiceDetectedCount,averageDwellMs,hintPlaybackCount,hintNoResponseCount,familyContributionCount\n");
        for (ReminiscenceMetricResult record : records) {
            csv.append(record.metricDate()).append(',')
                    .append(record.sessionCount()).append(',')
                    .append(record.voiceDetectedCount()).append(',')
                    .append(record.averageDwellMs()).append(',')
                    .append(record.hintPlaybackCount()).append(',')
                    .append(record.hintNoResponseCount()).append(',')
                    .append(record.familyContributionCount()).append('\n');
        }
        return csv.toString();
    }

    private void requireAssigned(UUID memberId, UUID elderId, InstitutionAuditAction action) {
        requireOperator(memberId, elderId, action);
        if (!assignments.existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(elderId, memberId)) {
            auditLogs.save(InstitutionPortalAuditLog.record(memberId, elderId, InstitutionAuditAction.ACCESS_DENIED, false));
            throw new M0AccessDeniedException("해당 어르신에 배정된 기관 담당자만 조회할 수 있어요.");
        }
    }

    private void requireOperator(UUID memberId, UUID elderId, InstitutionAuditAction action) {
        boolean authorized = members.findById(memberId)
                .filter(member -> member.isActive() && member.getRole() == MemberRole.INSTITUTION_ADMIN
                        && member.isTotpEnabled())
                .isPresent();
        auditLogs.save(InstitutionPortalAuditLog.record(memberId, elderId, authorized ? action : InstitutionAuditAction.ACCESS_DENIED,
                authorized));
        if (!authorized) {
            throw new M0AccessDeniedException("TOTP가 등록된 활성 기관 담당자만 포털을 이용할 수 있어요.");
        }
    }

    private LocalDate lastParticipationDate(Elder elder) {
        return metrics.findByElderIdAndDateBetween(elder.getId().toString(), LocalDate.now().minusMonths(MAX_LOOKBACK_MONTHS),
                        LocalDate.now()).stream()
                .filter(metric -> metric.participated())
                .map(metric -> metric.getMetricDate())
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.isBefore(LocalDate.now().minusMonths(MAX_LOOKBACK_MONTHS))) {
            throw new com.memeboo2.haemi.m0.domain.model.M0ValidationException("기관 기록은 최근 12개월 범위에서만 조회할 수 있어요.");
        }
    }
}
