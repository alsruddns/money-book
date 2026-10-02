package com.moneybook.backend.admin.service;

import com.moneybook.backend.admin.dto.*;
import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.enums.*;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;

public interface AdminService {
    AdminMeResponse me(Authentication authentication);
    AdminPageResponse<AdminUserResponse> users(String keyword, UserStatus status, SystemRole role,
                                                int page, int size, Authentication authentication);
    AdminUserDetailResponse user(Long userUid, Authentication authentication);
    AdminUserDetailResponse changeStatus(Long userUid, UserStatus status, Authentication authentication);
    AdminUserDetailResponse changeRole(Long userUid, SystemRole role, Authentication authentication);
    AdminPageResponse<AdminMoneyBookResponse> moneyBooks(String keyword, Long ownerUid,
                                                           int page, int size, Authentication authentication);
    AdminMoneyBookDetailResponse moneyBook(Long moneyBookUid, Authentication authentication);
    AdminPageResponse<AdminActivityResponse> activities(Long moneyBookUid, Long actorUid, ActivityType type,
            ActivityTargetType target, LocalDate start, LocalDate end, int page, int size,
            Authentication authentication);
    AdminOverviewResponse overview(Authentication authentication);
    AdminPageResponse<AdminAuditLogResponse> auditLogs(Long actorUid, AdminAuditActionType action,
            AdminAuditTargetType target, LocalDate start, LocalDate end, int page, int size,
            Authentication authentication);
}
