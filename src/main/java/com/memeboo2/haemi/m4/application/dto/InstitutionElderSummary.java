package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;

import java.time.LocalDate;

/** 기관 포털 전용 최소 공개 정보. 건강정보·가족 추억 본문은 포함하지 않는다. */
public record InstitutionElderSummary(String elderId, String name, ElderStatus status,
                                      LocalDate lastParticipationDate) {
    public static InstitutionElderSummary from(Elder elder, LocalDate lastParticipationDate) {
        return new InstitutionElderSummary(elder.getId().toString(), elder.getName(), elder.getStatus(),
                lastParticipationDate);
    }
}
