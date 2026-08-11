package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.InstitutionAssignment;
import com.memeboo2.haemi.m0.domain.repository.InstitutionAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InstitutionAssignmentRepositoryAdapter implements InstitutionAssignmentRepository {

    private final JpaInstitutionAssignmentRepository assignments;

    @Override
    public InstitutionAssignment save(InstitutionAssignment assignment) {
        return assignments.save(assignment);
    }

    @Override
    public Optional<InstitutionAssignment> findByElderIdAndInstitutionAdminMemberId(
            UUID elderId, UUID institutionAdminMemberId) {
        return assignments.findByElderIdAndInstitutionAdminMemberId(elderId, institutionAdminMemberId);
    }

    @Override
    public List<InstitutionAssignment> findAllByElderIdAndActiveTrue(UUID elderId) {
        return assignments.findAllByElderIdAndActiveTrue(elderId);
    }

    @Override
    public List<InstitutionAssignment> findAllByInstitutionAdminMemberIdAndActiveTrue(UUID institutionAdminMemberId) {
        return assignments.findAllByInstitutionAdminMemberIdAndActiveTrue(institutionAdminMemberId);
    }

    @Override
    public boolean existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(
            UUID elderId, UUID institutionAdminMemberId) {
        return assignments.existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(elderId, institutionAdminMemberId);
    }
}
