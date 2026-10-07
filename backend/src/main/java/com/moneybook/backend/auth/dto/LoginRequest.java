package com.moneybook.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String loginId,
        @NotBlank @Size(max = 72) String password
) {
    @Override
    public String toString() {
        return "LoginRequest[loginId=REDACTED, password=REDACTED]";
    }
}
