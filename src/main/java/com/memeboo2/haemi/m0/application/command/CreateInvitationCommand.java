package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;

public record CreateInvitationCommand(String phoneNumber, FamilyRelation relation) {
}
