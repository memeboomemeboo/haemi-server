package com.memeboo2.haemi.eventlog.domain.repository;

import com.memeboo2.haemi.eventlog.domain.EventTypeCount;
import com.memeboo2.haemi.eventlog.domain.LoggedEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface LoggedEventRepository {

    LoggedEvent save(LoggedEvent event);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // 동의 철회 시 기존분 가명 처리, 처리 건수 반환
    int pseudonymizeByElderId(String elderId);

    // 06:00 일일 집계: 기간 내 타입별 카운트
    List<EventTypeCount> countByTypeBetween(LocalDateTime from, LocalDateTime to);
}
