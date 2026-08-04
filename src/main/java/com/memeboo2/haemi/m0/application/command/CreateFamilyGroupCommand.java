package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;

public record CreateFamilyGroupCommand(FamilyRelation relation, NotificationPreference notificationPreference) {
}
