package com.moneybook.backend.admin.dto;

import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailResponse(Long userUid, String loginId, String nickname, UserStatus status,
        SystemRole systemRole, LocalDateTime createdAt, LocalDateTime updatedAt,
        long ownedMoneyBookCount, long joinedMoneyBookCount, List<String> authProviders,
        long activeSessionCount, LocalDateTime lastActivityAt, long recentActivityCount) {
    public AdminUserDetailResponse(Long userUid, String loginId, String nickname, UserStatus status,
            SystemRole systemRole, LocalDateTime createdAt, LocalDateTime updatedAt,
            long ownedMoneyBookCount, long joinedMoneyBookCount) {
        this(userUid, loginId, nickname, status, systemRole, createdAt, updatedAt,
                ownedMoneyBookCount, joinedMoneyBookCount, List.of(), 0, null, 0);
    }
}
