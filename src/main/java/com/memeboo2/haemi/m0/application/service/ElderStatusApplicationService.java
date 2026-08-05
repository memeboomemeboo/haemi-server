package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.application.dto.ElderStatusResult;
import com.memeboo2.haemi.m0.domain.event.ElderBereavedEvent;
import com.memeboo2.haemi.m0.domain.event.ElderBereavementRecoveredEvent;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 상태 전이·사별 처리 (F0-05). 사별 확정/복구 시 도메인 이벤트를 발행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ElderStatusApplicationService {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${haemi.elder.bereavement.silent-days:7}")
    private int silentDays;

    @Value("${haemi.elder.bereavement.recovery-window-hours:48}")
    private int recoveryWindowHours;

    public ElderStatusResult changeStatus(UUID actorId, UUID elderId, ElderStatus target) {
        Elder elder = authorized(actorId, elderId);
        elder.transitionTo(target);
        return ElderStatusResult.from(elders.save(elder));
    }

    // 사별 1단계: 요청
    public ElderStatusResult requestBereavement(UUID actorId, UUID elderId) {
        Elder elder = authorized(actorId, elderId);
        elder.requestBereavement(LocalDateTime.now());
        return ElderStatusResult.from(elders.save(elder));
    }

    // 사별 2단계: 확정 → 이벤트 발행(잡 취소·기기 잠금·억제)
    public ElderStatusResult confirmBereavement(UUID actorId, UUID elderId) {
        Elder elder = authorized(actorId, elderId);
        elder.confirmBereavement(LocalDateTime.now(), silentDays);
        elders.save(elder);
        eventPublisher.publishEvent(new ElderBereavedEvent(
                elder.getId(), elder.getGroupId(), elder.getBereavedAt(), elder.getSilentUntil()));
        return ElderStatusResult.from(elder);
    }

    // 48시간 내 사별 오등록 복구
    public ElderStatusResult recoverBereavement(UUID actorId, UUID elderId) {
        Elder elder = authorized(actorId, elderId);
        elder.recoverFromBereavement(LocalDateTime.now(), recoveryWindowHours);
        elders.save(elder);
        eventPublisher.publishEvent(new ElderBereavementRecoveredEvent(
                elder.getId(), elder.getGroupId(), LocalDateTime.now()));
        return ElderStatusResult.from(elder);
    }

    // 무음기간 경과 후 memorial 봉인
    public ElderStatusResult enshrineMemorial(UUID actorId, UUID elderId) {
        Elder elder = authorized(actorId, elderId);
        elder.enshrineMemorial(LocalDateTime.now());
        return ElderStatusResult.from(elders.save(elder));
    }

    @Transactional(readOnly = true)
    public ElderStatusResult get(UUID actorId, UUID elderId) {
        return ElderStatusResult.from(authorized(actorId, elderId));
    }

    private Elder authorized(UUID actorId, UUID elderId) {
        Elder elder = elders.findById(elderId)
                .orElseThrow(() -> new M0NotFoundException("어르신 프로필"));
        FamilyGroup group = groups.findById(elder.getGroupId())
                .orElseThrow(() -> new M0NotFoundException("가족 그룹"));
        group.requireActiveMember(actorId);
        return elder;
    }
}
