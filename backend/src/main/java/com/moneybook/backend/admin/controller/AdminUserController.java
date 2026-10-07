package com.moneybook.backend.admin.controller;

import com.moneybook.backend.admin.dto.*;
import com.moneybook.backend.admin.service.AdminService;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 사용자 운영 정보 조회, 계정 상태 관리 및 Super Admin 전용 역할 관리 API. */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminService service;

    /** 사용자 목록을 조건과 DB pagination으로 조회한다. */
    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminUserResponse>> users(
            @RequestParam(required=false) String keyword, @RequestParam(required=false) UserStatus status,
            @RequestParam(required=false) SystemRole systemRole, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size, Authentication authentication) {
        return ResponseEntity.ok(service.users(keyword,status,systemRole,page,size,authentication));
    }

    /** 사용자 운영 정보와 가계부 개수만 조회하며 상세 조회 Audit을 남긴다. */
    @GetMapping("/{userUid}")
    public ResponseEntity<AdminUserDetailResponse> user(@PathVariable Long userUid, Authentication authentication) {
        return ResponseEntity.ok(service.user(userUid,authentication));
    }

    /** Returns a user's actor activity timeline with the standard bounded admin pagination. */
    @GetMapping("/{userUid}/activities")
    public ResponseEntity<AdminPageResponse<AdminActivityResponse>> activities(@PathVariable Long userUid,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(service.userActivities(userUid, page, size, authentication));
    }

    /** Revokes every active refresh session for an operationally affected user. */
    @PostMapping("/{userUid}/sessions/revoke-all")
    public ResponseEntity<Integer> revokeAllSessions(@PathVariable Long userUid, Authentication authentication) {
        return ResponseEntity.ok(service.revokeAllUserSessions(userUid, authentication));
    }

    /** 관리자 정책에 허용된 사용자 상태를 ACTIVE 또는 BLOCKED로 변경한다. */
    @PatchMapping("/{userUid}/status")
    public ResponseEntity<AdminUserDetailResponse> status(@PathVariable Long userUid,
            @Valid @RequestBody ChangeUserStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.changeStatus(userUid,request.status(),authentication));
    }

    /** Super Admin만 USER와 SYSTEM_ADMIN 사이의 역할을 변경할 수 있다. */
    @PatchMapping("/{userUid}/system-role")
    public ResponseEntity<AdminUserDetailResponse> role(@PathVariable Long userUid,
            @Valid @RequestBody ChangeSystemRoleRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.changeRole(userUid,request.systemRole(),authentication));
    }
}
