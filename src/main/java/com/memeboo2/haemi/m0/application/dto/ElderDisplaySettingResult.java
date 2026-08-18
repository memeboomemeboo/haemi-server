package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.ElderDisplaySetting;

import java.util.UUID;

public record ElderDisplaySettingResult(
        UUID elderId,
        int fontSizeLevel,
        boolean voiceFeatureEnabled,
        boolean notificationEnabled
) {
    public static ElderDisplaySettingResult from(ElderDisplaySetting s) {
        return new ElderDisplaySettingResult(s.getElderId(), s.getFontSizeLevel(),
                s.isVoiceFeatureEnabled(), s.isNotificationEnabled());
    }
}
