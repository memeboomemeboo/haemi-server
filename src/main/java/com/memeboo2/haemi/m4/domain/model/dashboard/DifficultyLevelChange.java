package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "difficulty_level_changes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DifficultyLevelChange {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "album_id", nullable = false, columnDefinition = "uuid")
    private UUID albumId;

    @Column(name = "previous_level", nullable = false)
    private int previousLevel;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "three_session_moving_average", nullable = false)
    private double threeSessionMovingAverage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "difficulty_level_change_wrong_questions",
            joinColumns = @JoinColumn(name = "change_id")
    )
    @Column(name = "question_id", nullable = false)
    private List<String> repeatedWrongQuestionIds = new ArrayList<>();

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public static DifficultyLevelChange create(
            UUID sessionId,
            String elderId,
            UUID albumId,
            int previousLevel,
            int currentLevel,
            double threeSessionMovingAverage,
            List<String> repeatedWrongQuestionIds,
            LocalDateTime changedAt
    ) {
        DifficultyLevelChange change = new DifficultyLevelChange();
        change.id = UUID.randomUUID();
        change.sessionId = sessionId;
        change.elderId = elderId;
        change.albumId = albumId;
        change.previousLevel = previousLevel;
        change.currentLevel = currentLevel;
        change.threeSessionMovingAverage = threeSessionMovingAverage;
        change.repeatedWrongQuestionIds.addAll(repeatedWrongQuestionIds);
        change.changedAt = changedAt;
        return change;
    }

    public List<String> getRepeatedWrongQuestionIds() {
        return Collections.unmodifiableList(repeatedWrongQuestionIds);
    }
}
