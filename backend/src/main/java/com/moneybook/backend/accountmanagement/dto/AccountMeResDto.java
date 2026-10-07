package com.moneybook.backend.accountmanagement.dto;

import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

/** 계정 관리 화면에서 사용하는 민감 인증정보 제외 사용자 정보. */
public record AccountMeResDto(
        Long userUid,
        String nickname,
        UserStatus status,
        SystemRole systemRole,
        List<AuthProvider> providers,
        String loginId,
        LocalDateTime regTime,
        LocalDateTime modTime
) {
}
