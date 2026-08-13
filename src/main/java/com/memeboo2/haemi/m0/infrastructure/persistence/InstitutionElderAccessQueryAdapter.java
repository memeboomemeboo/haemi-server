package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.port.InstitutionElderAccessQuery;
import com.memeboo2.haemi.m0.domain.repository.InstitutionAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InstitutionElderAccessQueryAdapter implements InstitutionElderAccessQuery {

    private final InstitutionAssignmentRepository assignments;
    private final MemberRepository members;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAssignment(String elderId, UUID institutionAdminMemberId) {
        if (elderId == null || institutionAdminMemberId == null) {
            return false;
        }
        try {
            UUID parsedElderId = UUID.fromString(elderId);
            boolean isActiveInstitutionAdmin = members.findById(institutionAdminMemberId)
                    .map(member -> member.isActive() && member.getRole() == MemberRole.INSTITUTION_ADMIN)
                    .orElse(false);
            return isActiveInstitutionAdmin
                    && assignments.existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(
                    parsedElderId, institutionAdminMemberId);
        } catch (IllegalArgumentException invalidUuid) {
            return false;
        }
    }
}
