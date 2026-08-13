package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/** 기관 관리자에게 명시적으로 배정된 어르신인지 확인하는 인가 계약. */
public interface InstitutionElderAccessQuery {

    boolean hasActiveAssignment(String elderId, UUID institutionAdminMemberId);
}
