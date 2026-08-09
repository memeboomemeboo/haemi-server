package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/** 사진-인물 태깅의 테넌트 경계를 보장하는 M1 연동 계약. */
public interface PhotoOwnershipPort {
    void requireBelongsToGroup(UUID photoId, UUID groupId);
}
