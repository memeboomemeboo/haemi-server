package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.application.dto.AccessModeResult;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.model.access.AccessModeAssessment;
import com.memeboo2.haemi.m0.domain.model.access.AccessModeRecommendation;
import com.memeboo2.haemi.m0.domain.model.access.EntryPath;
import com.memeboo2.haemi.m0.domain.model.access.RecommendationSource;
import com.memeboo2.haemi.m0.domain.model.access.RecommendationStatus;
import com.memeboo2.haemi.m0.domain.repository.AccessModeRecommendationRepository;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 접근 모드 진단·추천·적용 (F0-03). 재평가는 제안까지만이며, 적용은 명시적 가족 행동으로만 이뤄진다.
 * 적용 시 프로필 등 데이터는 그대로 승계된다(재온보딩 없음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccessModeApplicationService {

    private final ElderRepository elders;
    private final FamilyGroupRepository groups;
    private final AccessModeRecommendationRepository recommendations;

    @Value("${haemi.access-mode.review-interval-days:14}")
    private int reviewIntervalDays;

    // 진단 5문항 → 추천 제안(PROPOSED)
    public AccessModeResult assess(UUID actorId, UUID elderId, List<Integer> answers) {
        Elder elder = authorized(actorId, elderId);
        ElderAccessMode recommended = AccessModeAssessment.recommend(answers);
        AccessModeRecommendation reco = recommendations.save(
                AccessModeRecommendation.propose(elderId, recommended, RecommendationSource.INITIAL));
        return AccessModeResult.of(elder.getAccessMode(), reco);
    }

    // 추천 적용: 모드 변경 + 대행 실행 기록. 데이터는 승계(리셋 없음).
    public AccessModeResult applyRecommendation(UUID actorId, UUID elderId, UUID recommendationId,
                                                EntryPath entryPath, UUID operatorId) {
        Elder elder = authorized(actorId, elderId);
        AccessModeRecommendation reco = recommendations.findById(recommendationId)
                .orElseThrow(() -> new M0NotFoundException("접근 모드 추천"));
        if (!reco.getElderId().equals(elderId)) {
            throw new M0NotFoundException("접근 모드 추천");
        }
        reco.apply(entryPath, operatorId, LocalDateTime.now());
        elder.changeAccessMode(reco.getRecommendedMode()); // 데이터 승계 — 프로필/생애정보 유지
        elders.save(elder);
        recommendations.save(reco);
        log.info("접근 모드 적용: elderId={}, mode={}, entryPath={}, operatorId={}",
                elderId, reco.getRecommendedMode(), entryPath, operatorId);
        return AccessModeResult.of(elder.getAccessMode(), reco);
    }

    public AccessModeResult dismissRecommendation(UUID actorId, UUID elderId, UUID recommendationId) {
        Elder elder = authorized(actorId, elderId);
        AccessModeRecommendation reco = recommendations.findById(recommendationId)
                .orElseThrow(() -> new M0NotFoundException("접근 모드 추천"));
        reco.dismiss();
        return AccessModeResult.of(elder.getAccessMode(), recommendations.save(reco));
    }

    @Transactional(readOnly = true)
    public AccessModeResult getLatest(UUID actorId, UUID elderId) {
        Elder elder = authorized(actorId, elderId);
        AccessModeRecommendation reco = recommendations.findLatestByElderId(elderId)
                .orElseThrow(() -> new M0NotFoundException("접근 모드 추천"));
        return AccessModeResult.of(elder.getAccessMode(), reco);
    }

    // 14일 주기 재평가: 대상 어르신에 재평가 제안만 생성(모드 변경 없음)
    public int runPeriodicReview(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(reviewIntervalDays);
        List<UUID> dueElderIds = recommendations.findElderIdsAppliedBefore(cutoff);
        int proposed = 0;
        for (UUID elderId : dueElderIds) {
            AccessModeRecommendation latest = recommendations.findLatestByElderId(elderId).orElse(null);
            // 이미 열린 제안이 있으면 중복 생성하지 않음
            if (latest == null || latest.getStatus() != RecommendationStatus.APPLIED) {
                continue;
            }
            elders.findById(elderId).ifPresent(elder ->
                    recommendations.save(AccessModeRecommendation.propose(
                            elderId, elder.getAccessMode(), RecommendationSource.PERIODIC_REVIEW)));
            proposed++;
        }
        if (proposed > 0) {
            log.info("접근 모드 14일 재평가 제안 생성: {}건", proposed);
        }
        return proposed;
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
