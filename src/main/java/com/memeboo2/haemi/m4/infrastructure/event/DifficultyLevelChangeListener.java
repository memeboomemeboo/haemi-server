package com.memeboo2.haemi.m4.infrastructure.event;

import com.memeboo2.haemi.m3.domain.event.DifficultyLevelChangedEvent;
import com.memeboo2.haemi.m4.domain.model.dashboard.DifficultyLevelChange;
import com.memeboo2.haemi.m4.domain.repository.DifficultyLevelChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DifficultyLevelChangeListener {

    private final DifficultyLevelChangeRepository repository;

    @EventListener
    public void onDifficultyLevelChanged(DifficultyLevelChangedEvent event) {
        repository.save(DifficultyLevelChange.create(
                event.sessionId(),
                event.elderId(),
                event.albumId(),
                event.previousLevel(),
                event.currentLevel(),
                event.threeSessionMovingAverage(),
                event.repeatedWrongQuestionIds(),
                event.changedAt()
        ));
    }
}
