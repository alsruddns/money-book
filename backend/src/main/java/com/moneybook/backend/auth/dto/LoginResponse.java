package com.moneybook.backend.auth.dto;

public record LoginResponse(Long userUid, String nickname, String accessToken, String refreshToken) {
    @Override
    public String toString() {
        return "LoginResponse[userUid=" + userUid + ", nickname=REDACTED, tokens=REDACTED]";
    }
}
