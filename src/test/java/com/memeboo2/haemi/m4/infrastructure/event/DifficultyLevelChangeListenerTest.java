package com.memeboo2.haemi.m4.infrastructure.event;

import com.memeboo2.haemi.m3.domain.event.DifficultyLevelChangedEvent;
import com.memeboo2.haemi.m4.domain.model.dashboard.DifficultyLevelChange;
import com.memeboo2.haemi.m4.domain.repository.DifficultyLevelChangeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DifficultyLevelChangeListenerTest {

    @Mock DifficultyLevelChangeRepository repository;

    @Test
    void persistsDifficultyChangeForDashboardHistory() {
        DifficultyLevelChangeListener listener = new DifficultyLevelChangeListener(repository);
        UUID sessionId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();

        listener.onDifficultyLevelChanged(new DifficultyLevelChangedEvent(
                sessionId,
                "elder",
                albumId,
                2,
                3,
                0.83,
                List.of("question-1"),
                LocalDateTime.of(2026, 7, 6, 10, 0)
        ));

        ArgumentCaptor<DifficultyLevelChange> captor =
                ArgumentCaptor.forClass(DifficultyLevelChange.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().getPreviousLevel()).isEqualTo(2);
        assertThat(captor.getValue().getCurrentLevel()).isEqualTo(3);
        assertThat(captor.getValue().getRepeatedWrongQuestionIds())
                .containsExactly("question-1");
    }
}
