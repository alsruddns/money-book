package com.moneybook.backend.admin.dto;

import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import java.time.LocalDateTime;

public record AdminUserResponse(Long userUid, String loginId, String nickname, UserStatus status,
                                SystemRole systemRole, LocalDateTime createdAt) { }
