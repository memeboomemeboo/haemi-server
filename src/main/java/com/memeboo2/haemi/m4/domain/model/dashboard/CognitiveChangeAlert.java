package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cognitive_change_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CognitiveChangeAlert {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "album_id", columnDefinition = "uuid")
    private UUID albumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "guide_link")
    private String guideLink;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "false_positive_at")
    private LocalDateTime falsePositiveAt;

    public static CognitiveChangeAlert create(String elderId, UUID albumId, AlertType alertType,
                                              String message, String guideLink) {
        CognitiveChangeAlert alert = new CognitiveChangeAlert();
        alert.id = UUID.randomUUID();
        alert.elderId = elderId;
        alert.albumId = albumId;
        alert.alertType = alertType;
        alert.message = message;
        alert.guideLink = guideLink;
        alert.sentAt = LocalDateTime.now();
        return alert;
    }

    /** 기관 담당자의 오탐 피드백은 이후 안내 임계값을 보수적으로 조정하는 근거가 된다. */
    public void markFalsePositive(LocalDateTime now) {
        if (falsePositiveAt == null) {
            falsePositiveAt = now;
        }
    }
}
