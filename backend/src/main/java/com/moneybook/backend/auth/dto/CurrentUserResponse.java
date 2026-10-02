package com.moneybook.backend.auth.dto;

import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.enums.SystemRole;

public record CurrentUserResponse(Long userUid, String nickname, UserStatus status, SystemRole systemRole) {
}
