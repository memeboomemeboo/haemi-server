package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "elder_display_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderDisplaySetting {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "font_size_level", nullable = false)
    private int fontSizeLevel;

    @Column(name = "voice_feature_enabled", nullable = false)
    private boolean voiceFeatureEnabled;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ElderDisplaySetting createDefault(UUID elderId) {
        ElderDisplaySetting s = new ElderDisplaySetting();
        s.elderId = elderId;
        s.fontSizeLevel = 1;
        s.voiceFeatureEnabled = true;
        s.notificationEnabled = true;
        s.createdAt = LocalDateTime.now();
        s.updatedAt = s.createdAt;
        return s;
    }

    public void update(Integer fontSizeLevel, Boolean voiceFeatureEnabled, Boolean notificationEnabled) {
        if (fontSizeLevel != null) {
            if (fontSizeLevel < 1 || fontSizeLevel > 3) {
                throw new M0ValidationException("폰트 크기 레벨은 1~3 사이여야 해요.");
            }
            this.fontSizeLevel = fontSizeLevel;
        }
        if (voiceFeatureEnabled != null) {
            this.voiceFeatureEnabled = voiceFeatureEnabled;
        }
        if (notificationEnabled != null) {
            this.notificationEnabled = notificationEnabled;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
