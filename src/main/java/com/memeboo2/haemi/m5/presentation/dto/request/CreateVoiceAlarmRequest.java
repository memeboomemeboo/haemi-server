package com.memeboo2.haemi.m5.presentation.dto.request;

import com.memeboo2.haemi.m5.domain.model.care.AlarmType;
import com.memeboo2.haemi.m5.domain.model.care.RepeatRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateVoiceAlarmRequest(
        @NotBlank(message = "어르신 ID는 필수입니다.")
        String elderId,
        @NotBlank(message = "가족 그룹 ID는 필수입니다.")
        String groupId,
        @NotNull(message = "알람 유형은 필수입니다.")
        AlarmType alarmType,
        @NotNull(message = "알람 시간은 필수입니다.")
        LocalTime alarmTime,
        @NotNull(message = "반복 주기는 필수입니다.")
        RepeatRule repeatRule
) {}
