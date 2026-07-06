package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientSetting;

import java.time.LocalDateTime;
import java.util.Set;

public record AlertRecipientSettingResult(
        String elderId,
        String primaryCaregiverMemberId,
        Set<String> institutionManagerMemberIds,
        Set<String> allRecipientMemberIds,
        LocalDateTime updatedAt
) {
    public static AlertRecipientSettingResult from(AlertRecipientSetting setting) {
        return new AlertRecipientSettingResult(
                setting.getElderId(),
                setting.getPrimaryCaregiverMemberId(),
                setting.getInstitutionManagerMemberIds(),
                setting.recipientMemberIds(),
                setting.getUpdatedAt()
        );
    }
}
