package com.moneybook.backend.auth.dto;

public record LoginResponse(Long userUid, String nickname, String accessToken, String refreshToken) {
}
