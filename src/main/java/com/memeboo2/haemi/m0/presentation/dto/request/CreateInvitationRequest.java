package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.InvitationKind;
import jakarta.validation.constraints.Email;

/**
 * 가족 초대는 이메일·관계가 필수이고, 어르신 초대(kind=ELDER)는 두 값을 받지 않는다.
 * kind별 필수값 검증은 애플리케이션 서비스에서 수행한다.
 */
public record CreateInvitationRequest(InvitationKind kind, @Email String email, FamilyRelation relation) {
}
