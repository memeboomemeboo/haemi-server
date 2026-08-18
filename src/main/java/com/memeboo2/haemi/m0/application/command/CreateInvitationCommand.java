package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.InvitationKind;

/** kind=FAMILY는 이메일·관계가 필수이고, kind=ELDER는 6자리 코드만 발급한다. */
public record CreateInvitationCommand(InvitationKind kind, String email, FamilyRelation relation) {
}
