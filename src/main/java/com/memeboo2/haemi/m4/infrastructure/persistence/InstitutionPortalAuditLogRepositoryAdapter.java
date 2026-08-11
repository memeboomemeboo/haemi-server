package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.InstitutionPortalAuditLog;
import com.memeboo2.haemi.m4.domain.repository.InstitutionPortalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InstitutionPortalAuditLogRepositoryAdapter implements InstitutionPortalAuditLogRepository {

    private final JpaInstitutionPortalAuditLogRepository auditLogs;

    @Override
    public InstitutionPortalAuditLog save(InstitutionPortalAuditLog auditLog) {
        return auditLogs.save(auditLog);
    }
}
