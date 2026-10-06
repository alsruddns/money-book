package com.moneybook.backend.auth.dto;

public record LoginResponse(Long userUid, String nickname, String accessToken, String refreshToken,
                            boolean passwordChangeRequired) {
    public LoginResponse(Long uid,String nickname,String access,String refresh){this(uid,nickname,access,refresh,false);}
    @Override
    public String toString() {
        return "LoginResponse[userUid=" + userUid + ", nickname=REDACTED, tokens=REDACTED]";
    }
}
