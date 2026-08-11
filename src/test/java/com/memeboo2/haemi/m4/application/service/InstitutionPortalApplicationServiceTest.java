package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.InstitutionAssignment;
import com.memeboo2.haemi.m0.domain.model.M0AccessDeniedException;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.InstitutionAssignmentRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import com.memeboo2.haemi.m4.domain.repository.InstitutionPortalAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionPortalApplicationServiceTest {

    @Mock MemberRepository members;
    @Mock InstitutionAssignmentRepository assignments;
    @Mock ElderRepository elders;
    @Mock CognitiveMetricRepository metrics;
    @Mock InstitutionPortalAuditLogRepository auditLogs;

    private InstitutionPortalApplicationService service;
    private Member manager;
    private Elder elder;

    @BeforeEach
    void setUp() {
        service = new InstitutionPortalApplicationService(members, assignments, elders, metrics, auditLogs);
        manager = Member.create("staff@example.com", "password", "담당자", MemberRole.INSTITUTION_ADMIN);
        manager.enableTotp("secret");
        elder = Elder.create(UUID.randomUUID(), "care-home", "김어르신", 1945,
                Gender.FEMALE, ResidenceType.HOME_WITH_FAMILY);
        lenient().when(members.findById(manager.getId())).thenReturn(Optional.of(manager));
        lenient().when(metrics.findByElderIdAndDateBetween(anyString(), any(), any())).thenReturn(List.of());
    }

    @Test
    void listsOnlyAssignedNonMemorialEldersAfterTotpCheck() {
        InstitutionAssignment assignment = InstitutionAssignment.assign(elder.getId(), "care-home", manager.getId(), UUID.randomUUID());
        when(assignments.findAllByInstitutionAdminMemberIdAndActiveTrue(manager.getId())).thenReturn(List.of(assignment));
        when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));

        var result = service.listAssigned(manager.getId());

        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.elderId()).isEqualTo(elder.getId().toString());
            assertThat(summary.name()).isEqualTo("김어르신");
        });
        verify(auditLogs).save(any());
    }

    @Test
    void deniesRecordForUnassignedInstitutionManagerAndAuditsIt() {
        when(assignments.existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(elder.getId(), manager.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getRecord(manager.getId(), elder.getId(),
                LocalDate.now().minusDays(7), LocalDate.now()))
                .isInstanceOf(M0AccessDeniedException.class);

        verify(auditLogs, times(2)).save(any());
    }

    @Test
    void blocksRecordOlderThanTwelveMonthsBeforeLookup() {
        assertThatThrownBy(() -> service.getRecord(manager.getId(), elder.getId(),
                LocalDate.now().minusMonths(12).minusDays(1), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(assignments);
    }
}
