package com.moneybook.backend.auth.dto;

import com.moneybook.backend.enums.UserStatus;

public record CurrentUserResponse(Long userUid, String nickname, UserStatus status) {
}
