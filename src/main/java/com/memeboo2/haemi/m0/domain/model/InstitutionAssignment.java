package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "institution_assignments", uniqueConstraints = @UniqueConstraint(
        name = "uk_institution_assignments_elder_admin",
        columnNames = {"elder_id", "institution_admin_member_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitutionAssignment {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "institution_id", nullable = false, length = 100)
    private String institutionId;

    @Column(name = "institution_admin_member_id", nullable = false, columnDefinition = "uuid")
    private UUID institutionAdminMemberId;

    @Column(name = "assigned_by_member_id", nullable = false, columnDefinition = "uuid")
    private UUID assignedByMemberId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static InstitutionAssignment assign(UUID elderId, String institutionId,
                                                UUID institutionAdminMemberId, UUID assignedByMemberId) {
        validate(institutionId, institutionAdminMemberId);
        InstitutionAssignment assignment = new InstitutionAssignment();
        assignment.id = UUID.randomUUID();
        assignment.elderId = elderId;
        assignment.institutionId = institutionId.trim();
        assignment.institutionAdminMemberId = institutionAdminMemberId;
        assignment.assignedByMemberId = assignedByMemberId;
        assignment.active = true;
        assignment.assignedAt = LocalDateTime.now();
        return assignment;
    }

    public void reactivate(String institutionId, UUID assignedByMemberId) {
        validate(institutionId, institutionAdminMemberId);
        this.institutionId = institutionId.trim();
        this.assignedByMemberId = assignedByMemberId;
        this.active = true;
        this.assignedAt = LocalDateTime.now();
        this.revokedAt = null;
    }

    public void revoke() {
        if (!active) {
            return;
        }
        this.active = false;
        this.revokedAt = LocalDateTime.now();
    }

    private static void validate(String institutionId, UUID institutionAdminMemberId) {
        if (institutionId == null || institutionId.isBlank() || institutionId.trim().length() > 100) {
            throw new M0ValidationException("기관 ID는 1~100자로 입력해주세요.");
        }
        if (institutionAdminMemberId == null) {
            throw new M0ValidationException("기관 관리자 계정은 필수예요.");
        }
    }
}
