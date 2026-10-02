package com.moneybook.backend.admin.service.impl;

import com.moneybook.backend.activity.repository.ActivityRepository;
import com.moneybook.backend.admin.dto.*;
import com.moneybook.backend.admin.enums.*;
import com.moneybook.backend.admin.provider.AdminAuditRecorder;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.admin.repository.AdminAuditRepository;
import com.moneybook.backend.admin.repository.AdminReadRepository;
import com.moneybook.backend.admin.service.AdminService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.enums.*;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

/** DB 상태를 재검증하고 제한된 운영 조회·변경 기능을 제공한다. */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final SystemAdminAuthorizationProvider authorization;
    private final AdminReadRepository reads;
    private final AdminAuditRepository audits;
    private final ActivityRepository activities;
    private final UserRepository users;
    private final AdminAuditRecorder auditRecorder;

    @Override @Transactional(readOnly = true)
    public AdminMeResponse me(Authentication authentication) {
        User actor = authorization.requireAdmin(authentication);
        return new AdminMeResponse(actor.getUserUid(), actor.getNickname(), actor.getSystemRole());
    }

    @Override @Transactional(readOnly = true)
    public AdminPageResponse<AdminUserResponse> users(String keyword, UserStatus status, SystemRole role,
            int page, int size, Authentication authentication) {
        authorization.requireAdmin(authentication);
        validateKeyword(keyword, 100);
        var result = reads.users(keyword, status, role, pageable(page, size).request());
        return AdminPageResponse.from(result);
    }

    @Override @Transactional
    public AdminUserDetailResponse user(Long uid, Authentication authentication) {
        User actor = authorization.requireAdmin(authentication);
        User target = target(uid);
        protectAdminDetails(actor, target);
        AdminUserDetailResponse detail = reads.user(uid).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        auditRecorder.record(actor, AdminAuditActionType.USER_DETAIL_VIEWED, AdminAuditTargetType.USER,
                uid, "사용자 운영 정보를 조회했습니다.");
        return detail;
    }

    @Override @Transactional
    public AdminUserDetailResponse changeStatus(Long uid, UserStatus requested, Authentication authentication) {
        User actor = authorization.requireAdmin(authentication);
        User target = target(uid);
        if (actor.getUserUid().equals(uid)) throw new BusinessException(ErrorCode.SELF_ADMIN_MODIFICATION_FORBIDDEN);
        protectTargetRole(actor, target);
        if (requested != UserStatus.ACTIVE && requested != UserStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (target.getStatus() == requested) return reads.user(uid).orElseThrow();
        target.changeStatus(requested);
        auditRecorder.record(actor, AdminAuditActionType.USER_STATUS_CHANGED, AdminAuditTargetType.USER,
                uid, requested == UserStatus.BLOCKED ? "사용자를 정지했습니다." : "사용자 정지를 해제했습니다.");
        return reads.user(uid).orElseThrow();
    }

    @Override @Transactional
    public AdminUserDetailResponse changeRole(Long uid, SystemRole requested, Authentication authentication) {
        User actor = authorization.requireSuperAdmin(authentication);
        User target = target(uid);
        if (actor.getUserUid().equals(uid)) throw new BusinessException(ErrorCode.SELF_ADMIN_MODIFICATION_FORBIDDEN);
        if (target.getSystemRole() == SystemRole.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_MODIFICATION_FORBIDDEN);
        }
        if (requested == SystemRole.SUPER_ADMIN || requested == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ROLE_CHANGE_FORBIDDEN);
        }
        if (target.getSystemRole() == requested) return reads.user(uid).orElseThrow();
        target.changeSystemRole(requested);
        auditRecorder.record(actor, AdminAuditActionType.USER_SYSTEM_ROLE_CHANGED, AdminAuditTargetType.USER,
                uid, requested == SystemRole.SYSTEM_ADMIN ? "서비스 관리자를 지정했습니다." : "서비스 관리자 권한을 회수했습니다.");
        return reads.user(uid).orElseThrow();
    }

    @Override @Transactional(readOnly = true)
    public AdminPageResponse<AdminMoneyBookResponse> moneyBooks(String keyword, Long ownerUid,
            int page, int size, Authentication authentication) {
        authorization.requireAdmin(authentication);
        validateKeyword(keyword, 100);
        return AdminPageResponse.from(reads.moneyBooks(keyword, ownerUid, pageable(page, size).request()));
    }

    @Override @Transactional
    public AdminMoneyBookDetailResponse moneyBook(Long uid, Authentication authentication) {
        User actor = authorization.requireAdmin(authentication);
        AdminMoneyBookDetailResponse detail = reads.moneyBook(uid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MONEY_BOOK_NOT_FOUND));
        auditRecorder.record(actor, AdminAuditActionType.MONEY_BOOK_DETAIL_VIEWED,
                AdminAuditTargetType.MONEY_BOOK, uid, "가계부 운영 정보를 조회했습니다.");
        return detail;
    }

    @Override @Transactional
    public AdminPageResponse<AdminActivityResponse> activities(Long bookUid, Long actorUid, ActivityType type,
            ActivityTargetType target, LocalDate start, LocalDate end, int page, int size,
            Authentication authentication) {
        authorization.requireAdmin(authentication);
        PageableData p = pageable(page, size);
        validateDates(start, end);
        var sorted = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("activityUid")));
        var rows = activities.search(bookUid, start, end, actorUid, type, target, sorted);
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : reads.moneyBookNames(rows.getContent().stream()
                .map(a -> a.getMoneyBookUid()).distinct().toList())) names.put((Long) row[0], (String) row[1]);
        var content = rows.getContent().stream().map(a -> new AdminActivityResponse(
                a.getActivityUid(), a.getMoneyBookUid(), names.get(a.getMoneyBookUid()), a.getActorUserUid(),
                a.getActorNickname(), a.getActivityType().name(), a.getTargetType().name(),
                a.getTargetUid(), a.getSummary(), a.getMetadataJson(), a.getOccurredAt())).toList();
        User actor = authorization.requireAdmin(authentication);
        auditRecorder.record(actor, AdminAuditActionType.ADMIN_ACTIVITY_SEARCHED,
                AdminAuditTargetType.ACTIVITY, bookUid, "가계부 활동 내역을 검색했습니다.");
        return AdminPageResponse.from(new PageImpl<>(content, sorted, rows.getTotalElements()));
    }

    @Override @Transactional(readOnly = true)
    public AdminOverviewResponse overview(Authentication authentication) {
        authorization.requireAdmin(authentication);
        return reads.overview();
    }

    @Override @Transactional
    public AdminPageResponse<AdminAuditLogResponse> auditLogs(Long actorUid, AdminAuditActionType action,
            AdminAuditTargetType target, LocalDate start, LocalDate end, int page, int size,
            Authentication authentication) {
        authorization.requireSuperAdmin(authentication);
        PageableData p = pageable(page, size);
        validateDates(start, end);
        var sorted = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("adminAuditLogUid")));
        var result = audits.search(actorUid, action, target, start, end, sorted)
                .map(AdminAuditLogResponse::from);
        return AdminPageResponse.from(result);
    }

    private User target(Long uid) {
        return users.findById(uid).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    private void protectAdminDetails(User actor, User target) {
        if (actor.getSystemRole() == SystemRole.SYSTEM_ADMIN && target.getSystemRole() != SystemRole.USER) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_TARGET_FORBIDDEN);
        }
    }
    private void protectTargetRole(User actor, User target) {
        if (target.getSystemRole() == SystemRole.SUPER_ADMIN
                || (actor.getSystemRole() == SystemRole.SYSTEM_ADMIN
                && target.getSystemRole() != SystemRole.USER)) {
            throw new BusinessException(ErrorCode.SYSTEM_ADMIN_TARGET_FORBIDDEN);
        }
    }
    private PageableData pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return new PageableData(PageRequest.of(page, size));
    }
    private void validateKeyword(String keyword, int maxLength) {
        if (keyword != null && keyword.length() > maxLength) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
    private void validateDates(LocalDate start, LocalDate end) {
        if ((start != null && end != null && start.isAfter(end)) || LocalDate.MAX.equals(end)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
    private record PageableData(org.springframework.data.domain.Pageable request) { }
}
