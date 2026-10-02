package com.moneybook.backend.admin.provider;

import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** 현재 DB 상태를 기준으로 시스템 관리자 권한과 활성 상태를 확인한다. */
@Component
@RequiredArgsConstructor
public class SystemAdminAuthorizationProvider {
    private final UserRepository users;

    public boolean hasAdminAccess(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken)) return false;
        Long uid = uid(authentication);
        return uid != null && users.findById(uid)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> user.getSystemRole() == SystemRole.SYSTEM_ADMIN
                        || user.getSystemRole() == SystemRole.SUPER_ADMIN).orElse(false);
    }

    public User requireAdmin(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_ACCESS_DENIED);
        }
        Long uid = uid(authentication);
        User user = uid == null ? null : users.findById(uid).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || (user.getSystemRole() != SystemRole.SYSTEM_ADMIN
                && user.getSystemRole() != SystemRole.SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_ACCESS_DENIED);
        }
        return user;
    }

    public User requireSuperAdmin(Authentication authentication) {
        User user = requireAdmin(authentication);
        if (user.getSystemRole() != SystemRole.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_ACCESS_DENIED);
        }
        return user;
    }

    private Long uid(Authentication authentication) {
        try { return Long.valueOf(authentication.getName()); }
        catch (RuntimeException ignored) { return null; }
    }
}
