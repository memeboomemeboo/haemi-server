package com.memeboo2.haemi.regression;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m3.application.dto.TrainingSessionResult;
import com.memeboo2.haemi.m3.domain.model.hint.AccrualSource;
import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S1 회귀 스위트 (Developer B 담당분, #50 릴리스 게이트).
 *
 * <p>지금 충족되는 케이스는 실제 검증으로 고정하고, 어르신 상태 머신(#36)에 의존하는 케이스는
 * {@link Disabled} 스켈레톤으로 게이트를 문서화한다. #36 완료 시 스켈레톤을 실 구현으로 채운다.
 * CI 게이트는 Developer A와 공동 소유.
 */
class S1RegressionSuiteTest {

    private static final String PENDING_36 = "#36 어르신 상태 머신 선행 필요 · 릴리스 게이트 #50";

    @Nested
    @DisplayName("충족 (Developer B 소유 메커니즘)")
    class Satisfied {

        @Test
        @DisplayName("EX-F302-07: 세션 결과 DTO는 난이도/레벨을 어디에도 노출하지 않는다")
        void ex_f302_07_noDifficultyLevelExposed() {
            Set<String> names = recordComponentNamesDeep(TrainingSessionResult.class);

            assertThat(names)
                    .noneMatch(n -> n.toLowerCase().contains("level"))
                    .noneMatch(n -> n.toLowerCase().contains("difficulty"));
        }

        @Test
        @DisplayName("EX-F303-07: 숨김 인물 힌트는 억제 훅으로 비활성화되어 제공에서 제외된다")
        void ex_f303_07_hiddenPersonHintSuppressed() {
            AccruedHint hint = AccruedHint.accrue(
                    "elder-1", UUID.randomUUID(), "숨김인물",
                    AccrualSource.MEMO, "member-1", "손녀", "우리 같이 갔던 곳 기억나요?");
            assertThat(hint.isActive()).isTrue();

            hint.suppress();

            assertThat(hint.isActive()).isFalse();
        }

        @Test
        @DisplayName("EX-F303-08: 작고한 가족 힌트도 동일 억제 훅으로 제공에서 제외된다")
        void ex_f303_08_deceasedFamilyHintSuppressed() {
            AccruedHint hint = AccruedHint.accrue(
                    "elder-1", null, "작고가족",
                    AccrualSource.ONBOARDING, "member-2", "아들", "아버지가 좋아하시던 노래예요.");

            hint.suppress();

            assertThat(hint.isActive()).isFalse();
        }

        @Test
        @DisplayName("EX-F005-01: 사별 확정 후 어르신은 발송 최종 검증에서 제외된다 (#36으로 활성화)")
        void ex_f005_01_notDispatchableAfterBereavement() {
            Elder elder = Elder.create(UUID.randomUUID(), null, "김어르신", 1945,
                    Gender.FEMALE, ResidenceType.HOME_WITH_FAMILY);
            LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
            assertThat(elder.isDispatchable(now)).isTrue();

            elder.requestBereavement(now);
            elder.confirmBereavement(now, 7);

            assertThat(elder.isDispatchable(now)).isFalse();
        }
    }

    @Nested
    @DisplayName("#36 어르신 상태 머신 의존 (릴리스 게이트 대기)")
    class PendingElderStateMachine {

        @Test
        @Disabled(PENDING_36)
        @DisplayName("EX-F005-06: 사별 시 기기 잠금 실패는 복구 경로로 처리된다")
        void ex_f005_06_deviceLockFailureRecovers() {
            // seam: 사별 처리 실패 시 재시도/복구 큐
        }

        @Test
        @Disabled(PENDING_36)
        @DisplayName("EX-F201-05: 사별/입원 상태에서는 추억 알림이 차단된다")
        void ex_f201_05_memoryNotificationBlockedByStatus() {
            // seam: ElderNotificationPolicy에 상태 사유(NotificationBlockReason) 추가 후 검증
        }

        @Test
        @Disabled(PENDING_36)
        @DisplayName("EX-F301-10: 부적합 상태(사별/입원)에서는 세션 개시가 차단된다")
        void ex_f301_10_sessionStartBlockedForUnfitStatus() {
            // seam: 세션 개시 시 어르신 상태 조회 → 부적합이면 개시 거부
        }

        @Test
        @Disabled(PENDING_36)
        @DisplayName("EX-F301-11: 세션 중 사별 등록 시 세션이 즉시 종료된다")
        void ex_f301_11_sessionTerminatesOnBereavementMidSession() {
            // seam: 상태 전이 이벤트 → 진행 중 세션 즉시 종료
        }

        @Test
        @Disabled(PENDING_36)
        @DisplayName("EX-F501-06: 사별/입원 상태에서는 목소리 알람 발송이 차단된다")
        void ex_f501_06_voiceAlarmBlockedByStatus() {
            // seam: VoiceAlarm.isDispatchable() 앞단에 어르신 상태 조회 결합
        }

        @Test
        @Disabled(PENDING_36)
        @DisplayName("EX-F501-07: 작고한 가족의 음성 알람은 발송되지 않는다")
        void ex_f501_07_deceasedFamilyVoiceAlarmBlocked() {
            // seam: 로테이션 음성 중 작고 가족 음성 제외 + 대체
        }
    }

    /** 레코드와 중첩 레코드의 모든 컴포넌트 이름을 재귀 수집한다. */
    private static Set<String> recordComponentNamesDeep(Class<?> root) {
        Set<String> names = new HashSet<>();
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Class<?> type = stack.pop();
            if (!type.isRecord() || !visited.add(type)) {
                continue;
            }
            for (RecordComponent component : type.getRecordComponents()) {
                names.add(component.getName());
                Class<?> componentType = component.getType();
                if (componentType.isRecord()) {
                    stack.push(componentType);
                }
                // 중첩 제네릭(List<QuestionResult> 등)의 원소 레코드도 탐색
                for (Class<?> nested : type.getDeclaredClasses()) {
                    if (nested.isRecord()) {
                        stack.push(nested);
                    }
                }
            }
        }
        return names;
    }
}
