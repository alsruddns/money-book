package com.moneybook.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpReqDto(
        @NotBlank @Size(max = 100) String loginId,
        @NotBlank @Size(max = 72) String password,
        @NotBlank @Size(max = 72) String passwordConfirm,
        @NotBlank @Size(max = 50) String nickname
) {
    @Override
    public String toString() {
        return "SignUpReqDto[loginId=REDACTED, passwords=REDACTED, nickname=REDACTED]";
    }
}
