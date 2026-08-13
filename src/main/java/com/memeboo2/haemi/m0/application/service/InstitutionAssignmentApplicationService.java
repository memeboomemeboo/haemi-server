package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.application.dto.InstitutionAssignmentResult;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.InstitutionAssignment;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.model.M0ValidationException;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m0.domain.repository.InstitutionAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InstitutionAssignmentApplicationService {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;
    private final InstitutionAssignmentRepository assignments;
    private final MemberRepository members;

    public InstitutionAssignmentResult assign(UUID actorId, UUID elderId, String institutionId,
                                              UUID institutionAdminMemberId) {
        Elder elder = loadElder(elderId);
        groups.findById(elder.getGroupId()).orElseThrow(() -> new M0NotFoundException("가족 그룹"))
                .requireOwner(actorId);
        requireActiveInstitutionAdmin(institutionAdminMemberId);

        InstitutionAssignment assignment = assignments
                .findByElderIdAndInstitutionAdminMemberId(elderId, institutionAdminMemberId)
                .map(existing -> {
                    existing.reactivate(institutionId, actorId);
                    return existing;
                })
                .orElseGet(() -> InstitutionAssignment.assign(
                        elderId, institutionId, institutionAdminMemberId, actorId));
        return InstitutionAssignmentResult.from(assignments.save(assignment));
    }

    public void revoke(UUID actorId, UUID elderId, UUID institutionAdminMemberId) {
        Elder elder = loadElder(elderId);
        groups.findById(elder.getGroupId()).orElseThrow(() -> new M0NotFoundException("가족 그룹"))
                .requireOwner(actorId);
        InstitutionAssignment assignment = assignments
                .findByElderIdAndInstitutionAdminMemberId(elderId, institutionAdminMemberId)
                .orElseThrow(() -> new M0NotFoundException("기관 담당자 배정"));
        assignment.revoke();
        assignments.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<InstitutionAssignmentResult> findActive(UUID actorId, UUID elderId) {
        Elder elder = loadElder(elderId);
        groups.findById(elder.getGroupId()).orElseThrow(() -> new M0NotFoundException("가족 그룹"))
                .requireOwner(actorId);
        return assignments.findAllByElderIdAndActiveTrue(elderId).stream()
                .map(InstitutionAssignmentResult::from)
                .toList();
    }

    private Elder loadElder(UUID elderId) {
        return elders.findById(elderId).orElseThrow(() -> new M0NotFoundException("어르신 프로필"));
    }

    private void requireActiveInstitutionAdmin(UUID memberId) {
        members.findById(memberId)
                .filter(member -> member.isActive() && member.getRole() == MemberRole.INSTITUTION_ADMIN)
                .orElseThrow(() -> new M0ValidationException("활성 기관 관리자 계정만 배정할 수 있어요."));
    }
}
