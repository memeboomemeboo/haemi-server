package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/**
 * 어르신 관련 예약 잡(알람·선다운로드·훈련 알림 등) 일괄 취소 포트 (F0-05 사별 처리).
 */
public interface ScheduledJobCancelPort {

    void cancelAllForElder(UUID elderId);
}
