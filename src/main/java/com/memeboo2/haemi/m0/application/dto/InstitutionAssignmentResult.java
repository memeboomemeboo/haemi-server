package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.InstitutionAssignment;

import java.time.LocalDateTime;
import java.util.UUID;

public record InstitutionAssignmentResult(
        UUID assignmentId,
        UUID elderId,
        String institutionId,
        UUID institutionAdminMemberId,
        boolean active,
        LocalDateTime assignedAt,
        LocalDateTime revokedAt
) {
    public static InstitutionAssignmentResult from(InstitutionAssignment assignment) {
        return new InstitutionAssignmentResult(
                assignment.getId(),
                assignment.getElderId(),
                assignment.getInstitutionId(),
                assignment.getInstitutionAdminMemberId(),
                assignment.isActive(),
                assignment.getAssignedAt(),
                assignment.getRevokedAt()
        );
    }
}
