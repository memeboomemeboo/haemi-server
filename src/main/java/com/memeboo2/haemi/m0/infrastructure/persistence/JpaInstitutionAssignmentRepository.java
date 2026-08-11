package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.InstitutionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInstitutionAssignmentRepository extends JpaRepository<InstitutionAssignment, UUID> {

    Optional<InstitutionAssignment> findByElderIdAndInstitutionAdminMemberId(
            UUID elderId, UUID institutionAdminMemberId);

    List<InstitutionAssignment> findAllByElderIdAndActiveTrue(UUID elderId);

    boolean existsByElderIdAndInstitutionAdminMemberIdAndActiveTrue(
            UUID elderId, UUID institutionAdminMemberId);
}
