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
    @DisplayName("알림을 받을 가족 구성원이 없으면 손주 찬스를 요청할 수 없다")
    void requestGrandchildChance_rejectsEmptyRecipients() {
        CognitiveTrainingSession session = session();

        assertThatThrownBy(() -> session.requestGrandchildChance(Set.of()))
                .isInstanceOf(GrandchildChanceUnavailableException.class);
        assertThat(session.getChanceUsedCount()).isZero();
    }

    @Test
    @DisplayName("손주 찬스를 쓰지 않고 훈련을 완료하면 미사용 완료 뱃지를 수여한다")
    void complete_awardsChanceUnusedCompletionBadge() {
        CognitiveTrainingSession session = session();

        session.answer("q1", "a", 10);
        session.answer("q2", "b", 10);
        session.answer("q3", "c", 10);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.isChanceUnusedCompletionBadgeAwarded()).isTrue();
    }

    @Test
    @DisplayName("손주 찬스를 사용한 훈련 완료에는 미사용 완료 뱃지를 수여하지 않는다")
    void complete_doesNotAwardBadgeWhenChanceWasUsed() {
        CognitiveTrainingSession session = session();

        session.requestGrandchildChance(Set.of("family-1"));
        session.answer("q1", "a", 10);
        session.answer("q2", "b", 10);
        session.answer("q3", "c", 10);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
        assertThat(session.isChanceUnusedCompletionBadgeAwarded()).isFalse();
    }

    private CognitiveTrainingSession session() {
        return CognitiveTrainingSession.start(
                "elder-1",
                UUID.randomUUID(),
                StartMode.AUTO,
                2,
                List.of(
                        question("q1", QuestionType.WORD_ASSOCIATION, "a"),
                        question("q2", QuestionType.PERSON_RECALL, "b"),
                        question("q3", QuestionType.SEQUENCE_MEMORY, "c")
                )
        );
    }

    private TrainingQuestion question(String id, QuestionType type, String answer) {
        return TrainingQuestion.of(id, type, "문제", answer, 2);
    }
}
