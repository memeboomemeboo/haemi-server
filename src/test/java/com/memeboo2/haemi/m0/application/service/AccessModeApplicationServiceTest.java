package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.application.dto.AccessModeResult;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.model.access.AccessModeRecommendation;
import com.memeboo2.haemi.m0.domain.model.access.EntryPath;
import com.memeboo2.haemi.m0.domain.model.access.RecommendationSource;
import com.memeboo2.haemi.m0.domain.model.access.RecommendationStatus;
import com.memeboo2.haemi.m0.domain.repository.AccessModeRecommendationRepository;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessModeApplicationServiceTest {

    @Mock ElderRepository elders;
    @Mock FamilyGroupRepository groups;
    @Mock AccessModeRecommendationRepository recommendations;

    AccessModeApplicationService service;

    private final UUID actorId = UUID.randomUUID();
    private Elder elder;

    @BeforeEach
    void setUp() {
        service = new AccessModeApplicationService(elders, groups, recommendations);
        ReflectionTestUtils.setField(service, "reviewIntervalDays", 14);
        elder = Elder.create(UUID.randomUUID(), null, "김어르신", 1945,
                Gender.FEMALE, ResidenceType.HOME_WITH_FAMILY);
        lenient().when(elders.findById(elder.getId())).thenReturn(Optional.of(elder));
        lenient().when(groups.findById(elder.getGroupId())).thenReturn(Optional.of(mock(FamilyGroup.class)));
    }

    @Test
    @DisplayName("진단 제출은 추천을 제안(PROPOSED)으로 저장한다")
    void assess_proposesRecommendation() {
        when(recommendations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccessModeResult result = service.assess(actorId, elder.getId(), List.of(1, 1, 1, 1, 1)); // 5 → B

        assertThat(result.recommendedMode()).isEqualTo(ElderAccessMode.B);
        assertThat(result.status()).isEqualTo(RecommendationStatus.PROPOSED);
    }

    @Test
    @DisplayName("추천 적용은 모드를 바꾸고 프로필 데이터는 승계한다(재온보딩 없음)")
    void apply_changesModeAndPreservesData() {
        AccessModeRecommendation reco = AccessModeRecommendation.propose(
                elder.getId(), ElderAccessMode.A, RecommendationSource.INITIAL);
        when(recommendations.findById(reco.getId())).thenReturn(Optional.of(reco));
        when(recommendations.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(elders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccessModeResult result = service.applyRecommendation(
                actorId, elder.getId(), reco.getId(), EntryPath.CAREGIVER, actorId);

        assertThat(result.currentMode()).isEqualTo(ElderAccessMode.A);
        assertThat(result.status()).isEqualTo(RecommendationStatus.APPLIED);
        assertThat(result.entryPath()).isEqualTo(EntryPath.CAREGIVER);
        // 데이터 승계: 프로필 필드 유지
        assertThat(elder.getName()).isEqualTo("김어르신");
        assertThat(elder.getBirthYear()).isEqualTo(1945);
    }

    @Test
    @DisplayName("14일 재평가는 대상 어르신에 제안만 생성한다(모드 변경 없음)")
    void runPeriodicReview_proposesOnly() {
        AccessModeRecommendation applied = AccessModeRecommendation.propose(
                elder.getId(), ElderAccessMode.A, RecommendationSource.INITIAL);
        applied.apply(EntryPath.SELF, null, java.time.LocalDateTime.now());
        when(recommendations.findElderIdsAppliedBefore(any())).thenReturn(List.of(elder.getId()));
        when(recommendations.findLatestByElderId(elder.getId())).thenReturn(Optional.of(applied));
        when(recommendations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int proposed = service.runPeriodicReview(java.time.LocalDateTime.now());

        assertThat(proposed).isEqualTo(1);
        assertThat(elder.getAccessMode()).isEqualTo(ElderAccessMode.UNSET); // 스케줄러는 모드를 바꾸지 않음
        verify(recommendations).save(argThat(r ->
                r.getSource() == RecommendationSource.PERIODIC_REVIEW
                        && r.getStatus() == RecommendationStatus.PROPOSED));
    }
}
