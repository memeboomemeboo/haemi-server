package com.memeboo2.haemi.m3.domain.model.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitiveTrainingSessionTest {

    @Test
    @DisplayName("손주 찬스 요청 후 30분이 지나면 힌트 전달을 거부하고 만료 상태로 처리한다")
    void applyHint_rejectsExpiredGrandchildChance() {
        CognitiveTrainingSession session = session();
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 6, 10, 0);

        session.requestGrandchildChance(Set.of("family-1"), requestedAt);

        assertThatThrownBy(() -> session.applyHint("손녀", "첫 글자는 사입니다", requestedAt.plusMinutes(30)))
                .isInstanceOf(GrandchildChanceExpiredException.class);
        assertThat(session.isLastGrandchildChanceExpired()).isTrue();
        assertThat(session.getLastHintText()).isNull();
    }

    @Test
    @DisplayName("손주 찬스 요청 후 30분 이내에는 힌트를 적용한다")
    void applyHint_acceptsResponseWithinThirtyMinutes() {
        CognitiveTrainingSession session = session();
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 6, 10, 0);

        session.requestGrandchildChance(Set.of("family-1"), requestedAt);
        session.applyHint("손녀", "첫 글자는 사입니다", requestedAt.plusMinutes(29));

        assertThat(session.getLastChanceStatus()).isEqualTo(GrandchildChanceStatus.ANSWERED);
        assertThat(session.getLastHintResponder()).isEqualTo("손녀");
        assertThat(session.getLastHintText()).isEqualTo("첫 글자는 사입니다");
    }

    @Test
    @DisplayName("30분이 지난 손주 찬스는 조회 갱신 후 안내와 문제 패스를 허용한다")
    void refreshAndPass_expiredGrandchildChance() {
        CognitiveTrainingSession session = session();
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 6, 10, 0);
        session.requestGrandchildChance(Set.of("family-1"), requestedAt);

        boolean changed = session.refreshGrandchildChanceStatus(requestedAt.plusMinutes(30));
        QuestionAttempt attempt = session.passCurrentQuestion();

        assertThat(changed).isTrue();
        assertThat(session.isLastGrandchildChanceExpired()).isTrue();
        assertThat(attempt.isResponded()).isFalse();
        assertThat(attempt.isTimeout()).isTrue();
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("30분이 지나지 않은 손주 찬스 문제는 건너뛸 수 없다")
    void pass_rejectsPendingGrandchildChance() {
        CognitiveTrainingSession session = session();
        session.requestGrandchildChance(Set.of("family-1"));

        assertThatThrownBy(session::passCurrentQuestion)
                .isInstanceOf(TrainingQuestionPassUnavailableException.class);
    }

    @Test
    @DisplayName("알림을 받을 가족 구성원이 없으면 손주 찬스를 요청할 수 없다")
    void requestGrandchildChance_rejectsEmptyRecipients() {
        CognitiveTrainingSession session = session();

        assertThatThrownBy(() -> session.requestGrandchildChance(Set.of()))
                .isInstanceOf(GrandchildChanceUnavailableException.class);
        assertThat(session.getChanceUsedCount()).isZero();
    }

    @Test
    @DisplayName("손주 찬스를 쓰지 않고 회상을 완료하면 미사용 완료 뱃지를 수여한다")
    void complete_awardsChanceUnusedCompletionBadge() {
        CognitiveTrainingSession session = session();

        session.answer("q1", "a", 10);
        session.answer("q2", "b", 10);
        session.answer("q3", "c", 10);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.isChanceUnusedCompletionBadgeAwarded()).isTrue();
    }

    @Test
    @DisplayName("손주 찬스를 사용한 회상 완료에는 미사용 완료 뱃지를 수여하지 않는다")
    void complete_doesNotAwardBadgeWhenChanceWasUsed() {
        CognitiveTrainingSession session = session();

        session.requestGrandchildChance(Set.of("family-1"));
        session.answer("q1", "a", 10);
        session.answer("q2", "b", 10);
        session.answer("q3", "c", 10);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.isChanceUnusedCompletionBadgeAwarded()).isFalse();
    }

    @Test
    @DisplayName("세션 시작 시 난이도를 1~5 범위로 보정한다")
    void start_clampsDifficultyLevel() {
        CognitiveTrainingSession low = session(0);
        CognitiveTrainingSession high = session(10);

        assertThat(low.getDifficultyLevel()).isEqualTo(1);
        assertThat(high.getDifficultyLevel()).isEqualTo(5);
        assertThat(low.getStatus()).isEqualTo(TrainingSessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("문제는 3~5개여야 하며 같은 유형이 연속될 수 없다")
    void start_validatesQuestions() {
        assertThatThrownBy(() -> CognitiveTrainingSession.start(
                "elder", UUID.randomUUID(), StartMode.AUTO, 2, questions().subList(0, 2)))
                .isInstanceOf(IllegalArgumentException.class);

        List<TrainingQuestion> repeated = List.of(
                question("q1", QuestionType.PERSON_RECALL),
                question("q2", QuestionType.PERSON_RECALL),
                question("q3", QuestionType.PLACE_MATCH));
        assertThatThrownBy(() -> CognitiveTrainingSession.start(
                "elder", UUID.randomUUID(), StartMode.AUTO, 2, repeated))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("현재 문제에만 응답할 수 있고 발화가 있으면 응답으로 기록한다")
    void answer_validatesCurrentQuestionAndRecordsResponse() {
        CognitiveTrainingSession session = session(2);

        assertThatThrownBy(() -> session.answer("q2", "이야기", 10))
                .isInstanceOf(TrainingQuestionNotFoundException.class);

        QuestionAttempt attempt = session.answer("q1", "  손녀와 함께한 날  ", 10);
        assertThat(attempt.isResponded()).isTrue();
        assertThat(session.currentQuestion()).get().extracting(TrainingQuestion::getQuestionId)
                .isEqualTo("q2");
    }

    @Test
    @DisplayName("마지막 문제에 응답하면 세션을 완료하고 응답률과 평균 응답 시간을 계산한다")
    void answer_lastQuestionCompletesSession() {
        CognitiveTrainingSession session = session(2);

        session.answer("q1", "이야기", 10);
        session.answer("q2", "", 20);
        session.answer("q3", "추억", 30);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.currentQuestion()).isEmpty();
        assertThat(session.getRespondedCount()).isEqualTo(2);
        assertThat(session.getNoResponseCount()).isEqualTo(1);
        assertThat(session.getResponseRate()).isEqualTo(2.0 / 3.0);
        assertThat(session.getAverageResponseSeconds()).isEqualTo(20.0);
        assertThatThrownBy(() -> session.answer("q3", "추억", 10))
                .isInstanceOf(TrainingSessionAlreadyCompletedException.class);
    }

    @Test
    @DisplayName("손주 찬스는 세션당 두 번만 사용할 수 있다")
    void requestGrandchildChance_enforcesLimit() {
        CognitiveTrainingSession session = session(2);
        Set<String> family = Set.of("family-1");

        assertThat(session.requestGrandchildChance(family)).isEqualTo(1);
        assertThat(session.requestGrandchildChance(family)).isZero();
        assertThatThrownBy(() -> session.requestGrandchildChance(family))
                .isInstanceOf(GrandchildChanceExhaustedException.class);
    }

    @Test
    @DisplayName("빈 힌트는 적용할 수 없다")
    void applyHint_rejectsBlankText() {
        CognitiveTrainingSession session = session(2);

        assertThatThrownBy(() -> session.applyHint("손녀", " "))
                .isInstanceOf(IllegalArgumentException.class);

        session.requestGrandchildChance(Set.of("family-1"));
        session.applyHint("손녀", "첫 글자는 사입니다");
    }

    @Test
    @DisplayName("응답 시간은 음수가 되지 않고 60초 초과는 시간 초과로 본다")
    void questionAttempt_normalizesResponseTime() {
        QuestionAttempt negative = QuestionAttempt.of("q1", "a", true, -1);
        QuestionAttempt timeout = QuestionAttempt.of("q1", "a", true, 61);

        assertThat(negative.getResponseSeconds()).isZero();
        assertThat(negative.isTimeout()).isFalse();
        assertThat(timeout.isTimeout()).isTrue();
    }

    @Test
    @DisplayName("빠른 응답 3회가 연속되면 난이도를 높인다")
    void difficultyProfile_increasesAfterConsecutiveResponses() {
        DifficultyProfile profile = DifficultyProfile.defaultFor("elder");

        profile.applySession(
                List.of(
                        performance("q1", QuestionType.PERSON_RECALL, true, false),
                        performance("q2", QuestionType.PLACE_MATCH, true, false),
                        performance("q3", QuestionType.COLOR_SHAPE, true, false)
                ),
                1.0,
                10.0,
                DifficultyPolicy.defaultFor(2)
        );

        assertThat(profile.getCurrentLevel()).isEqualTo(3);
        assertThat(profile.getConsecutiveResponded()).isZero();
    }

    @Test
    @DisplayName("무응답 3회 또는 시간 초과가 있으면 난이도를 낮춘다")
    void difficultyProfile_decreasesAfterNoResponses() {
        DifficultyProfile noResponse = DifficultyProfile.defaultFor("elder-1");
        noResponse.applySession(
                List.of(
                        performance("q1", QuestionType.PERSON_RECALL, false, false),
                        performance("q2", QuestionType.PLACE_MATCH, false, false),
                        performance("q3", QuestionType.COLOR_SHAPE, false, false)
                ),
                0.0,
                10.0,
                DifficultyPolicy.defaultFor(2)
        );

        DifficultyProfile timeout = DifficultyProfile.defaultFor("elder-2");
        timeout.applySession(
                List.of(performance(
                        "q1", QuestionType.PERSON_RECALL, false, true)),
                0.0,
                61.0,
                DifficultyPolicy.defaultFor(2)
        );

        assertThat(noResponse.getCurrentLevel()).isEqualTo(1);
        assertThat(timeout.getCurrentLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("0%에서 100%로 급변한 두 번째 세션은 난이도를 즉시 높이지 않는다")
    void difficultyProfile_buffersExtremeScoreUntilThreeSessionsExist() {
        DifficultyProfile profile = DifficultyProfile.defaultFor("elder");
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(2);
        List<QuestionPerformance> neutral = List.of(
                performance("q1", QuestionType.PERSON_RECALL, true, false),
                performance("q2", QuestionType.PLACE_MATCH, false, false),
                performance("q3", QuestionType.COLOR_SHAPE, true, false)
        );

        profile.applySession(neutral, 0.0, 20.0, policy);
        DifficultyAdjustment adjustment = profile.applySession(
                List.of(
                        performance("q4", QuestionType.PERSON_RECALL, true, false),
                        performance("q5", QuestionType.PLACE_MATCH, true, false),
                        performance("q6", QuestionType.COLOR_SHAPE, true, false)
                ),
                1.0,
                10.0,
                policy
        );

        assertThat(adjustment.extremeScoreBuffered()).isTrue();
        assertThat(profile.getCurrentLevel()).isEqualTo(2);
        assertThat(profile.getRecentResponseRates()).containsExactly(0.0, 1.0);
    }

    @Test
    @DisplayName("응답률 이력은 최근 세 세션만 유지한다")
    void difficultyProfile_keepsOnlyThreeSessionWindow() {
        DifficultyProfile profile = DifficultyProfile.defaultFor("elder");
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(2);
        List<QuestionPerformance> neutral = List.of(
                performance("q1", QuestionType.PERSON_RECALL, true, false),
                performance("q2", QuestionType.PLACE_MATCH, false, false)
        );

        profile.applySession(neutral, 0.1, 20.0, policy);
        profile.applySession(neutral, 0.2, 20.0, policy);
        profile.applySession(neutral, 0.3, 20.0, policy);
        profile.applySession(neutral, 0.4, 20.0, policy);

        assertThat(profile.getRecentResponseRates()).containsExactly(0.2, 0.3, 0.4);
        assertThat(profile.getThreeSessionMovingAverage()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("문제 유형 추천은 정책에 설정된 유형을 사용한다")
    void difficultyProfile_recommendsPolicyQuestionTypes() {
        DifficultyProfile profile = DifficultyProfile.defaultFor("elder");
        DifficultyPolicy policy = DifficultyPolicy.defaultFor(2);

        assertThat(profile.recommendQuestionTypes(policy))
                .containsExactlyInAnyOrderElementsOf(policy.getQuestionTypes());
    }

    @Test
    @DisplayName("60초 무응답 허용: 발화 없이 다음 문제로 진행하고 무응답으로 기록한다")
    void recordNoResponse_advancesWithoutResponse() {
        CognitiveTrainingSession session = session(2);

        QuestionAttempt attempt = session.recordNoResponse("q1");

        assertThat(attempt.isResponded()).isFalse();
        assertThat(attempt.isTimeout()).isFalse();
        assertThat(attempt.getResponseSeconds())
                .isEqualTo(CognitiveTrainingSession.NO_RESPONSE_ALLOWANCE_SECONDS);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("무응답 진행은 현재 문제에만 허용하며 마지막 문제면 세션을 완료한다")
    void recordNoResponse_validatesCurrentAndCompletes() {
        CognitiveTrainingSession session = session(2);

        assertThatThrownBy(() -> session.recordNoResponse("q2"))
                .isInstanceOf(TrainingQuestionNotFoundException.class);

        session.recordNoResponse("q1");
        session.answer("q2", "이야기", 10);
        session.recordNoResponse("q3");

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.getRespondedCount()).isEqualTo(1);
        assertThat(session.getNoResponseCount()).isEqualTo(2);
        assertThatThrownBy(() -> session.recordNoResponse("q3"))
                .isInstanceOf(TrainingSessionAlreadyCompletedException.class);
    }

    private CognitiveTrainingSession session() {
        return CognitiveTrainingSession.start(
                "elder-1",
                UUID.randomUUID(),
                StartMode.AUTO,
                2,
                List.of(
                        question("q1", QuestionType.PERSON_RECALL),
                        question("q2", QuestionType.PLACE_MATCH),
                        question("q3", QuestionType.COLOR_SHAPE)
                )
        );
    }

    private CognitiveTrainingSession session(int level) {
        return CognitiveTrainingSession.start(
                "elder", UUID.randomUUID(), StartMode.AUTO, level, questions());
    }

    private List<TrainingQuestion> questions() {
        return List.of(
                question("q1", QuestionType.PERSON_RECALL),
                question("q2", QuestionType.PLACE_MATCH),
                question("q3", QuestionType.COLOR_SHAPE));
    }

    private TrainingQuestion question(String id, QuestionType type) {
        return TrainingQuestion.of(id, type, "질문 " + id, 2);
    }

    private QuestionPerformance performance(
            String questionId,
            QuestionType type,
            boolean responded,
            boolean timeout
    ) {
        return new QuestionPerformance(questionId, type, responded, timeout);
    }
}
