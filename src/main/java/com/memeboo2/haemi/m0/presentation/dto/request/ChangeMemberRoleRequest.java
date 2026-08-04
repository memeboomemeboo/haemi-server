package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.GroupMemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(@NotNull GroupMemberRole role) {
}
