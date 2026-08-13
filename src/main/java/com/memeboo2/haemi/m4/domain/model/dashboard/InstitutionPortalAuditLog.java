package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "institution_portal_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitutionPortalAuditLog {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "member_id", nullable = false, columnDefinition = "uuid")
    private UUID memberId;

    @Column(name = "elder_id", columnDefinition = "uuid")
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InstitutionAuditAction action;

    @Column(nullable = false)
    private boolean allowed;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public static InstitutionPortalAuditLog record(UUID memberId, UUID elderId,
                                                   InstitutionAuditAction action, boolean allowed) {
        InstitutionPortalAuditLog log = new InstitutionPortalAuditLog();
        log.id = UUID.randomUUID();
        log.memberId = memberId;
        log.elderId = elderId;
        log.action = action;
        log.allowed = allowed;
        log.occurredAt = LocalDateTime.now();
        return log;
    }
}
