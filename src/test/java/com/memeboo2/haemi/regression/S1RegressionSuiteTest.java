package com.memeboo2.haemi.regression;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.PersonContentTense;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.event.PersonSafetyChangedEvent;
import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.PhotoFile;
import com.memeboo2.haemi.m1.domain.model.album.PhotoMetadata;
import com.memeboo2.haemi.m1.domain.model.memory.Memory;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationStatus;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryVisibility;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ContentSafetyValidator;
import com.memeboo2.haemi.m1.domain.model.reminiscence.ContentSafetyViolation;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m1.domain.repository.ReminiscenceContentRepository;
import com.memeboo2.haemi.m1.infrastructure.event.PersonSafetyInvalidationListener;
import com.memeboo2.haemi.m3.application.dto.TrainingSessionResult;
import com.memeboo2.haemi.m3.domain.model.hint.AccrualSource;
import com.memeboo2.haemi.m3.domain.model.hint.AccruedHint;
import com.memeboo2.haemi.m4.application.dto.ReminiscenceReportResult;
import com.memeboo2.haemi.m4.application.service.ActivityChangeLanguagePolicy;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportMode;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("충족 (Developer A 소유 메커니즘)")
    class DeveloperA {

        @Test
        @DisplayName("EX-F004-01: 작고한 인물의 현재형·모호한 언급은 카드 생성에서 차단된다")
        void ex_f004_01_deceasedPersonPresentOrAmbiguousReferenceIsBlocked() {
            var deceased = List.of(new PersonExposurePort.PhotoPersonExposure(
                    UUID.randomUUID(), "영희", null, PersonContentTense.PAST_ONLY, true));
            ContentSafetyValidator validator = new ContentSafetyValidator();

            assertThat(validator.validate("영희는 여기 있어요?", deceased, List.of()))
                    .contains(ContentSafetyViolation.DECEASED_PERSON_PRESENT_TENSE);
            assertThat(validator.validate("영희와 함께 남긴 사진이에요, 이야기를 들려주실래요?", deceased, List.of()))
                    .isEmpty();
        }

        @Test
        @DisplayName("EX-F004-04: 인물 노출 안전 상태가 바뀌면 기존 회상 카드를 즉시 무효화한다")
        void ex_f004_04_personSafetyChangeInvalidatesExistingCards() {
            AlbumRepository albums = mock(AlbumRepository.class);
            ReminiscenceContentRepository contents = mock(ReminiscenceContentRepository.class);
            UUID groupId = UUID.randomUUID();
            Album album = Album.create(UUID.randomUUID().toString(), groupId.toString(), UUID.randomUUID().toString());
            when(albums.findByGroupId(groupId.toString())).thenReturn(java.util.Optional.of(album));

            new PersonSafetyInvalidationListener(albums, contents)
                    .invalidate(new PersonSafetyChangedEvent(groupId, UUID.randomUUID()));

            verify(contents).invalidateByAlbumId(album.getAlbumId());
        }

        @Test
        @DisplayName("EX-F105-04: 분석 완료 사진이 20장 미만이면 회상 콘텐츠 생성 조건을 충족하지 못한다")
        void ex_f105_04_requiresTwentyAnalyzedPhotos() {
            Album album = Album.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "family-member");
            for (int index = 0; index < 19; index++) {
                var photo = album.addPhoto(PhotoFile.of("photo-" + index, "photo.jpg", "image/jpeg", 1),
                        PhotoMetadata.empty(), "hash-" + index, "family-member");
                album.markPhotoAnalysisCompleted(photo.getPhotoId());
            }

            assertThat(album.hasEnoughPhotosForAi(20)).isFalse();

            var twentieth = album.addPhoto(PhotoFile.of("photo-20", "photo.jpg", "image/jpeg", 1),
                    PhotoMetadata.empty(), "hash-20", "family-member");
            album.markPhotoAnalysisCompleted(twentieth.getPhotoId());
            assertThat(album.hasEnoughPhotosForAi(20)).isTrue();
        }

        @Test
        @DisplayName("EX-F105-05: 금기 주제와 퀴즈 어휘는 회상 콘텐츠 생성에서 함께 차단된다")
        void ex_f105_05_sensitiveTopicAndQuizVocabularyAreBlocked() {
            ContentSafetyValidator validator = new ContentSafetyValidator();

            assertThat(validator.validate("사별한 배우자 이야기를 들려주세요?", List.of(), List.of("사별한 배우자")))
                    .contains(ContentSafetyViolation.SENSITIVE_TOPIC);
            assertThat(validator.validate("이 사진 퀴즈를 풀어보실래요?", List.of(), List.of()))
                    .contains(ContentSafetyViolation.FORBIDDEN_TERM);
        }

        @Test
        @DisplayName("EX-F103-03: FAMILY_ONLY 또는 미검수 추억은 어르신 피드에서 노출되지 않는다")
        void ex_f103_03_onlyClearGroupMemoriesAreElderVisible() {
            UUID groupId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            Memory familyOnly = Memory.create(groupId, authorId, "가족 메모", "딸", "딸",
                    MemoryVisibility.FAMILY_ONLY, MemoryModerationStatus.CLEAR);
            Memory pending = Memory.create(groupId, authorId, "검수 전", "딸", "딸",
                    MemoryVisibility.GROUP_ALL, MemoryModerationStatus.REVIEW);
            Memory visible = Memory.create(groupId, authorId, "함께한 날", "딸", "딸",
                    MemoryVisibility.GROUP_ALL, MemoryModerationStatus.CLEAR);

            assertThat(familyOnly.isElderVisible()).isFalse();
            assertThat(pending.isElderVisible()).isFalse();
            assertThat(visible.isElderVisible()).isTrue();
        }

        @Test
        @DisplayName("EX-F401-05: 가족용 회상 리포트 DTO에는 점수·정답률·인지 평가 필드가 없다")
        void ex_f401_05_reportDtoDoesNotExposeCognitiveScores() {
            Set<String> names = recordComponentNamesDeep(ReminiscenceReportResult.class);

            assertThat(names)
                    .noneMatch(name -> name.toLowerCase().contains("score"))
                    .noneMatch(name -> name.toLowerCase().contains("accuracy"))
                    .noneMatch(name -> name.toLowerCase().contains("cognitive"));
        }

        @Test
        @DisplayName("EX-F401-06: memory_focused 리포트는 비교 안내 없이 회상 기록만 제공한다")
        void ex_f401_06_memoryFocusedReportOmitsActivityComparison() {
            CognitiveReport report = CognitiveReport.createReminiscence("elder-1", UUID.randomUUID(),
                    ReportPeriod.WEEKLY, java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 8, 7),
                    ReportMode.MEMORY_FOCUSED, 4, List.of("고향"), List.of("photo-1"), 2, 1,
                    null, "함께한 기억을 정리했어요.", ReportDeliveryMethod.IN_APP);

            ReminiscenceReportResult result = ReminiscenceReportResult.from(report);

            assertThat(result.mode()).isEqualTo(ReportMode.MEMORY_FOCUSED);
            assertThat(result.activityMessage()).isNull();
            assertThat(result.rememberedTopics()).containsExactly("고향");
        }

        @Test
        @DisplayName("EX-F402-06: 활동 변화 안내는 판정·치료·점수 표현을 발송 전에 차단한다")
        void ex_f402_06_activityLanguageGateBlocksClinicalOrScoringVocabulary() {
            ActivityChangeLanguagePolicy policy = new ActivityChangeLanguagePolicy();

            assertThatThrownBy(() -> policy.requireSafe("인지 기능이 악화되었습니다"))
                    .isInstanceOf(DomainValidationException.class);
            policy.requireSafe("이번 주에도 사진 이야기를 들려주셨어요.");
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
