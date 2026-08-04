package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;
import jakarta.validation.constraints.NotNull;

public record CreateGroupRequest(@NotNull FamilyRelation relation, NotificationPreference notificationPreference) {
}
