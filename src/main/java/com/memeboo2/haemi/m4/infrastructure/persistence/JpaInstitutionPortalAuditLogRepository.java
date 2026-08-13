package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.InstitutionPortalAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaInstitutionPortalAuditLogRepository extends JpaRepository<InstitutionPortalAuditLog, UUID> {
}
