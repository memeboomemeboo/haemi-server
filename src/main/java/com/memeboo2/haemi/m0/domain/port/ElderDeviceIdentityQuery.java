package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/** 어르신 프로필과 실제 ELDER 기기 계정의 연결 여부를 확인하는 다운스트림 계약. */
public interface ElderDeviceIdentityQuery {

    /** 연결되지 않았거나 형식이 올바르지 않은 ID는 안전하게 false를 반환한다. */
    boolean isLinkedElderMember(String elderId, UUID memberId);
}
