package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ElderStatusResult(
        UUID elderId,
        ElderStatus status,
        boolean bereavementPending,
        LocalDateTime bereavedAt,
        LocalDateTime silentUntil,
        boolean dispatchable,
        boolean memorialArchiveOnly
) {
    public static ElderStatusResult from(Elder elder) {
        return new ElderStatusResult(
                elder.getId(),
                elder.getStatus(),
                elder.isBereavementPending(),
                elder.getBereavedAt(),
                elder.getSilentUntil(),
                elder.isDispatchable(LocalDateTime.now()),
                elder.isMemorialArchiveOnly()
        );
    }
}
