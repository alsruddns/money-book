package com.moneybook.backend.enums;

public enum JwtTokenType {
    ACCESS,
    REFRESH;

    public static final String CLAIM_NAME = "token_type";
}
