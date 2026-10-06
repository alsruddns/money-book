package com.moneybook.backend.accountmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비밀번호 값이 기본 record toString()에 노출되지 않도록 표현을 재정의한다. */
public record AccountPasswordUpdateReqDto(
        @NotBlank @Size(max = 72) String currentPassword,
        @NotBlank @Size(max = 72) String newPassword,
        @NotBlank @Size(max = 72) String newPasswordConfirm
) {
    @Override
    public String toString() {
        return "AccountPasswordUpdateReqDto[passwords=REDACTED]";
    }
}
