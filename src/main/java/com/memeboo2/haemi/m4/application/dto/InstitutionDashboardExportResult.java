package com.memeboo2.haemi.m4.application.dto;

public record InstitutionDashboardExportResult(
        byte[] content,
        String contentType,
        String fileName
) {}
