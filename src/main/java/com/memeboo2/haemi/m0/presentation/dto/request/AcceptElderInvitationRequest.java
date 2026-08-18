package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 어르신 화면에서 1회 입력하는 항목과 기기 식별자 (F0-01-E).
 *
 * <p>그룹에 어르신 프로필이 이미 등록된 경우에는 birthYear·gender·residenceType을 무시하고
 * 이름 교차 검증을 진행한다. 프로필이 없는 경우에는 세 필드로 프로필을 자동 생성한다.
 */
public record AcceptElderInvitationRequest(
        @NotBlank @Size(min = 2, max = 20) String name,
        @NotBlank String phoneNumber,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "코드를 다시 확인해주세요.") String code,
        @NotBlank @Size(max = 128) String deviceId,
        @Min(1920) @Max(1970) Integer birthYear,
        Gender gender,
        ResidenceType residenceType
) {
}
