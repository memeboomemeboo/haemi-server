package com.memeboo2.haemi.m3.domain.model.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitiveTrainingSessionTest {

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
                question("q1", QuestionType.WORD_ASSOCIATION, "a"),
                question("q2", QuestionType.WORD_ASSOCIATION, "b"),
                question("q3", QuestionType.PERSON_RECALL, "c"));
        assertThatThrownBy(() -> CognitiveTrainingSession.start(
                "elder", UUID.randomUUID(), StartMode.AUTO, 2, repeated))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("현재 문제의 정답만 제출할 수 있고 정답은 공백과 대소문자를 무시한다")
    void answer_validatesCurrentQuestionAndAnswer() {
        CognitiveTrainingSession session = session(2);

        assertThatThrownBy(() -> session.answer("q2", "B", 10))
                .isInstanceOf(TrainingQuestionNotFoundException.class);

        QuestionAttempt attempt = session.answer("q1", "  A  ", 10);
        assertThat(attempt.isCorrect()).isTrue();
        assertThat(session.currentQuestion()).get().extracting(TrainingQuestion::getQuestionId)
                .isEqualTo("q2");
    }

    @Test
    @DisplayName("마지막 문제에 답하면 세션을 완료하고 정확도와 평균 응답 시간을 계산한다")
    void answer_lastQuestionCompletesSession() {
        CognitiveTrainingSession session = session(2);

        session.answer("q1", "a", 10);
        session.answer("q2", "wrong", 20);
        session.answer("q3", "c", 30);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.currentQuestion()).isEmpty();
        assertThat(session.getCorrectCount()).isEqualTo(2);
        assertThat(session.getWrongCount()).isEqualTo(1);
        assertThat(session.getAccuracyRate()).isEqualTo(2.0 / 3.0);
        assertThat(session.getAverageResponseSeconds()).isEqualTo(20.0);
        assertThatThrownBy(() -> session.answer("q3", "c", 10))
                .isInstanceOf(TrainingSessionAlreadyCompletedException.class);
    }

    @Test
    @DisplayName("손주 찬스는 세션당 두 번만 사용할 수 있다")
    void requestGrandchildChance_enforcesLimit() {
        CognitiveTrainingSession session = session(2);

        assertThat(session.requestGrandchildChance()).isEqualTo(1);
        assertThat(session.requestGrandchildChance()).isZero();
        assertThatThrownBy(session::requestGrandchildChance)
                .isInstanceOf(GrandchildChanceExhaustedException.class);
    }

    @Test
    @DisplayName("빈 힌트는 적용할 수 없다")
    void applyHint_rejectsBlankText() {
        CognitiveTrainingSession session = session(2);

        assertThatThrownBy(() -> session.applyHint("손녀", " "))
                .isInstanceOf(IllegalArgumentException.class);

        session.applyHint("손녀", "첫 글자는 사입니다");
        assertThat(session.getLastHintResponder()).isEqualTo("손녀");
        assertThat(session.getLastHintText()).isEqualTo("첫 글자는 사입니다");
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
    @DisplayName("빠른 정답 3회가 연속되면 난이도를 높인다")
    void difficultyProfile_increasesAfterConsecutiveCorrectAnswers() {
        DifficultyProfile profile = DifficultyProfile.defaultFor("elder");

        profile.applyAttempts(List.of(
                QuestionAttempt.of("q1", "a", true, 10),
                QuestionAttempt.of("q2", "b", true, 10),
                QuestionAttempt.of("q3", "c", true, 10)));

        assertThat(profile.getCurrentLevel()).isEqualTo(3);
        assertThat(profile.getConsecutiveCorrect()).isZero();
    }

    @Test
    @DisplayName("오답 3회 또는 시간 초과가 있으면 난이도를 낮춘다")
    void difficultyProfile_decreasesAfterFailures() {
        DifficultyProfile wrong = DifficultyProfile.defaultFor("elder-1");
        wrong.applyAttempts(List.of(
                QuestionAttempt.of("q1", "x", false, 10),
                QuestionAttempt.of("q2", "x", false, 10),
                QuestionAttempt.of("q3", "x", false, 10)));

        DifficultyProfile timeout = DifficultyProfile.defaultFor("elder-2");
        timeout.applyAttempts(List.of(QuestionAttempt.of("q1", "a", true, 61)));

        assertThat(wrong.getCurrentLevel()).isEqualTo(1);
        assertThat(timeout.getCurrentLevel()).isEqualTo(1);
    }

    private CognitiveTrainingSession session(int level) {
        return CognitiveTrainingSession.start(
                "elder", UUID.randomUUID(), StartMode.AUTO, level, questions());
    }

    private List<TrainingQuestion> questions() {
        return List.of(
                question("q1", QuestionType.WORD_ASSOCIATION, "a"),
                question("q2", QuestionType.PERSON_RECALL, "b"),
                question("q3", QuestionType.SEQUENCE_MEMORY, "c"));
    }

    private TrainingQuestion question(String id, QuestionType type, String answer) {
        return TrainingQuestion.of(id, type, "질문 " + id, answer, 2);
    }
}
