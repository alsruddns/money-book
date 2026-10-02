package com.moneybook.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=REDACTED]";
    }
}
