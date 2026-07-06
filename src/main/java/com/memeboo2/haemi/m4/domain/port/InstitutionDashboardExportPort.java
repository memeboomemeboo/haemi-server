package com.memeboo2.haemi.m4.domain.port;

import com.memeboo2.haemi.m4.application.dto.InstitutionDashboardExportResult;
import com.memeboo2.haemi.m4.application.dto.InstitutionDashboardResult;
import com.memeboo2.haemi.m4.domain.model.dashboard.DashboardExportFormat;

public interface InstitutionDashboardExportPort {

    InstitutionDashboardExportResult export(
            InstitutionDashboardResult dashboard,
            DashboardExportFormat format
    );
}
