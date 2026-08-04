package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cognitive_daily_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"elder_id", "metric_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CognitiveDailyMetric {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "album_id", columnDefinition = "uuid")
    private UUID albumId;

    @Column(name = "institution_id")
    private String institutionId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "training_session_count", nullable = false)
    private int trainingSessionCount;

    @Column(name = "training_accuracy_rate", nullable = false)
    private double trainingAccuracyRate;

    @Column(name = "average_response_seconds", nullable = false)
    private double averageResponseSeconds;

    @Column(name = "reminiscence_reaction_count", nullable = false)
    private int reminiscenceReactionCount;

    @Column(name = "memory_post_count", nullable = false)
    private int memoryPostCount;

    @Column(name = "most_reacted_photo_type")
    private String mostReactedPhotoType;

    @Column(name = "voice_detected_count", nullable = false)
    private int voiceDetectedCount;

    @Column(name = "average_dwell_ms", nullable = false)
    private double averageDwellMs;

    @Column(name = "hint_playback_count", nullable = false)
    private int hintPlaybackCount;

    @Column(name = "hint_no_response_count", nullable = false)
    private int hintNoResponseCount;

    @Column(name = "top_memory_topic", length = 100)
    private String topMemoryTopic;

    @Column(name = "top_dwelled_photo", length = 255)
    private String topDwelledPhoto;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CognitiveDailyMetric create(String elderId, UUID albumId, String institutionId,
                                              LocalDate metricDate, int trainingSessionCount,
                                              double trainingAccuracyRate, double averageResponseSeconds,
                                              int reminiscenceReactionCount, int memoryPostCount,
                                              String mostReactedPhotoType) {
        CognitiveDailyMetric metric = new CognitiveDailyMetric();
        metric.id = UUID.randomUUID();
        metric.elderId = elderId;
        metric.albumId = albumId;
        metric.institutionId = institutionId;
        metric.metricDate = metricDate;
        metric.trainingSessionCount = Math.max(trainingSessionCount, 0);
        metric.trainingAccuracyRate = clampRate(trainingAccuracyRate);
        metric.averageResponseSeconds = Math.max(averageResponseSeconds, 0.0);
        metric.reminiscenceReactionCount = Math.max(reminiscenceReactionCount, 0);
        metric.memoryPostCount = Math.max(memoryPostCount, 0);
        metric.mostReactedPhotoType = mostReactedPhotoType;
        metric.voiceDetectedCount = 0;
        metric.averageDwellMs = 0.0;
        metric.hintPlaybackCount = 0;
        metric.hintNoResponseCount = 0;
        metric.updatedAt = LocalDateTime.now();
        return metric;
    }

    public void mergeTrainingResult(UUID albumId, double accuracyRate, double averageResponseSeconds) {
        this.albumId = albumId;
        int previousCount = this.trainingSessionCount;
        this.trainingSessionCount++;
        this.trainingAccuracyRate = ((this.trainingAccuracyRate * previousCount) + clampRate(accuracyRate))
                / this.trainingSessionCount;
        this.averageResponseSeconds = ((this.averageResponseSeconds * previousCount)
                + Math.max(averageResponseSeconds, 0.0)) / this.trainingSessionCount;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSnapshot(String institutionId, int trainingSessionCount,
                               double trainingAccuracyRate, double averageResponseSeconds,
                               int reminiscenceReactionCount, int memoryPostCount,
                               String mostReactedPhotoType) {
        this.institutionId = institutionId;
        this.trainingSessionCount = Math.max(trainingSessionCount, 0);
        this.trainingAccuracyRate = clampRate(trainingAccuracyRate);
        this.averageResponseSeconds = Math.max(averageResponseSeconds, 0.0);
        this.reminiscenceReactionCount = Math.max(reminiscenceReactionCount, 0);
        this.memoryPostCount = Math.max(memoryPostCount, 0);
        this.mostReactedPhotoType = mostReactedPhotoType;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean participated() {
        return trainingSessionCount > 0 || reminiscenceReactionCount > 0 || memoryPostCount > 0;
    }

    public void updateReminiscenceSnapshot(String institutionId, int sessionCount, int voiceDetectedCount,
                                           double averageDwellMs, int hintPlaybackCount, int hintNoResponseCount,
                                           int familyContributionCount, String topMemoryTopic, String topDwelledPhoto) {
        this.institutionId = institutionId;
        this.trainingSessionCount = Math.max(sessionCount, 0);
        this.voiceDetectedCount = Math.max(voiceDetectedCount, 0);
        this.averageDwellMs = Math.max(averageDwellMs, 0.0);
        this.hintPlaybackCount = Math.max(hintPlaybackCount, 0);
        this.hintNoResponseCount = Math.min(this.hintPlaybackCount, Math.max(hintNoResponseCount, 0));
        this.memoryPostCount = Math.max(familyContributionCount, 0);
        this.topMemoryTopic = blankToNull(topMemoryTopic);
        this.topDwelledPhoto = blankToNull(topDwelledPhoto);
        this.updatedAt = LocalDateTime.now();
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
