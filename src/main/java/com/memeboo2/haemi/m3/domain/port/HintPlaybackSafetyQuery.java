package com.memeboo2.haemi.m3.domain.port;

import java.util.UUID;

/** F3-03 재생 직전에 숨김·사별 인물이 관련된 힌트를 차단하는 안전 조회 계약이다. */
public interface HintPlaybackSafetyQuery {

    boolean isPlayable(String elderId, UUID photoId, String authorMemberId, String mentionedPersonName);
}
