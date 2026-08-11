package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.application.command.*;
import com.memeboo2.haemi.m0.application.dto.*;
import com.memeboo2.haemi.m0.domain.model.*;
import com.memeboo2.haemi.m0.domain.port.InstitutionElderAccessQuery;
import com.memeboo2.haemi.m0.domain.repository.*;
import com.memeboo2.haemi.m0.infrastructure.security.ElderHealthCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ElderApplicationService {

    private final FamilyGroupRepository groups;
    private final ElderRepository elders;
    private final ElderHealthRepository elderHealth;
    private final LifeStoryRepository lifeStories;
    private final SensitiveTopicRepository sensitiveTopics;
    private final MemberRepository members;
    private final ElderHealthCrypto healthCrypto;
    private final InstitutionElderAccessQuery institutionAccess;

    public ElderResult create(UUID actorId, UUID groupId, CreateElderCommand command) {
        FamilyGroup group = loadGroup(groupId);
        group.requireActiveMember(actorId);
        if (elders.existsByGroupId(groupId)) {
            throw new M0ConflictException("이 가족 그룹에는 이미 어르신 프로필이 있어요.");
        }
        Elder elder = Elder.create(groupId, command.orgId(), command.name(), command.birthYear(), command.gender(),
                command.residenceType());
        elders.save(elder);
        return toResult(elder);
    }

    public ElderResult update(UUID actorId, UUID elderId, UpdateElderCommand command) {
        Elder elder = loadElder(elderId);
        requireProfileManager(actorId, elder);
        elder.updateProfile(command.name(), command.birthYear(), command.gender(), command.residenceType(), command.orgId());
        if (command.diagnosisLevel() != null) {
            loadGroup(elder.getGroupId()).requireOwner(actorId);
            String encryptedDiagnosis = healthCrypto.encrypt(command.diagnosisLevel().name());
            elderHealth.findByElderId(elderId)
                    .ifPresentOrElse(
                            health -> health.update(encryptedDiagnosis, command.healthConsentId(), command.diagnosedAt()),
                            () -> elderHealth.save(ElderHealth.create(elderId, encryptedDiagnosis,
                                    command.healthConsentId(), command.diagnosedAt()))
                    );
        }
        elders.save(elder);
        return toResult(elder);
    }

    public List<LifeStoryResult> replaceLifeStories(UUID actorId, UUID elderId, List<LifeStoryItem> items) {
        Elder elder = loadElder(elderId);
        requireProfileManager(actorId, elder);
        List<LifeStory> newStories = items == null ? List.of() : items.stream()
                .map(item -> LifeStory.create(elderId, item.category(), item.value(), item.weight(), item.source()))
                .toList();
        lifeStories.replaceAll(elderId, newStories);
        return newStories.stream().map(LifeStoryResult::from).toList();
    }

    public SensitiveTopicResult addSensitiveTopic(UUID actorId, UUID elderId, String keyword, String reason) {
        Elder elder = loadElder(elderId);
        requireProfileManager(actorId, elder);
        SensitiveTopic topic = SensitiveTopic.create(elderId, keyword, reason, actorId);
        return SensitiveTopicResult.from(sensitiveTopics.save(topic));
    }

    @Transactional(readOnly = true)
    public ElderCompletenessResult completeness(UUID actorId, UUID elderId) {
        Elder elder = loadElder(elderId);
        requireProfileManager(actorId, elder);
        return ElderCompletenessResult.from(elder.calculateCompleteness(lifeStories.findAllByElderId(elderId)));
    }

    public void withdrawHealthConsent(UUID actorId, UUID elderId) {
        Elder elder = loadElder(elderId);
        loadGroup(elder.getGroupId()).requireOwner(actorId);
        elderHealth.deleteByElderId(elderId);
    }

    /**
     * Mode A 자동 로그인에 사용할 어르신 계정을 연결한다. Mode B의 보호자 대행 기기와
     * 구분하기 위해, 기기 토큰은 이 값이 아니라 별도의 elderId로 연결된다.
     */
    public ElderResult linkMember(UUID actorId, UUID elderId, UUID elderMemberId) {
        Elder elder = loadElder(elderId);
        loadGroup(elder.getGroupId()).requireOwner(actorId);
        members.findById(elderMemberId)
                .filter(member -> member.isActive() && member.getRole() == MemberRole.ELDER)
                .orElseThrow(() -> new M0ValidationException("활성 어르신 계정만 연결할 수 있어요."));
        elders.findByMemberId(elderMemberId)
                .filter(other -> !other.getId().equals(elderId))
                .ifPresent(other -> {
                    throw new M0ConflictException("이미 다른 어르신 프로필에 연결된 계정이에요.");
                });
        elder.linkMember(elderMemberId);
        elders.save(elder);
        return toResult(elder);
    }

    @Transactional(readOnly = true)
    public ElderResult get(UUID actorId, UUID elderId) {
        Elder elder = loadElder(elderId);
        FamilyGroup group = loadGroup(elder.getGroupId());
        if (!group.isActiveMember(actorId)
                && !institutionAccess.hasActiveAssignment(elderId.toString(), actorId)) {
            throw new M0AccessDeniedException("가족 그룹 구성원 또는 배정된 기관 담당자만 조회할 수 있어요.");
        }
        return toResult(elder);
    }

    private ElderResult toResult(Elder elder) {
        return ElderResult.from(elder, elderHealth.findByElderId(elder.getId()).isPresent(),
                elder.calculateCompleteness(lifeStories.findAllByElderId(elder.getId())));
    }

    private Elder loadElder(UUID elderId) {
        return elders.findById(elderId).orElseThrow(() -> new M0NotFoundException("어르신 프로필"));
    }

    private FamilyGroup loadGroup(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> new M0NotFoundException("가족 그룹"));
    }

    private void requireProfileManager(UUID actorId, Elder elder) {
        if (loadGroup(elder.getGroupId()).isActiveMember(actorId)
                || institutionAccess.hasActiveAssignment(elder.getId().toString(), actorId)) {
            return;
        }
        throw new M0AccessDeniedException("가족 그룹 구성원 또는 배정된 기관 담당자만 프로필을 관리할 수 있어요.");
    }

}
