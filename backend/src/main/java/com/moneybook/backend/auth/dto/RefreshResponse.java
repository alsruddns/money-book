package com.moneybook.backend.auth.dto;

public record RefreshResponse(String accessToken, String refreshToken) {
    public RefreshResponse(String accessToken) {
        this(accessToken, null);
    }

    @Override
    public String toString() {
        return "RefreshResponse[tokens=REDACTED]";
    }
}
