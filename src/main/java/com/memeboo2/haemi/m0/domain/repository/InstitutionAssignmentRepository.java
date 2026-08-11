package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.InstitutionAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionAssignmentRepository {

    InstitutionAssignment save(InstitutionAssignment assignment);

    Optional<InstitutionAssignment> findByElderIdAndInstitutionAdminMemberId(UUID elderId, UUID institutionAdminMemberId);

    List<InstitutionAssignment> findAllByElderIdAndActiveTrue(UUID elderId);

    List<InstitutionAssignment> findAllByInstitutionAdminMemberIdAndActiveTrue(UUID institutionAdminMemberId);

    boolean existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(UUID elderId, UUID institutionAdminMemberId);
}
