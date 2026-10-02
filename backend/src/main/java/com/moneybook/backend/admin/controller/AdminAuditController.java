package com.moneybook.backend.admin.controller;

import com.moneybook.backend.admin.dto.AdminAuditLogResponse;
import com.moneybook.backend.admin.dto.AdminPageResponse;
import com.moneybook.backend.admin.enums.AdminAuditActionType;
import com.moneybook.backend.admin.enums.AdminAuditTargetType;
import com.moneybook.backend.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/** 최고 관리자만 운영 행위 감사 기록을 조회할 수 있는 API. */
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AdminService service;

    /** actor, action, target, 날짜 조건으로 immutable Admin Audit을 최신순 페이지 조회한다. */
    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminAuditLogResponse>> search(
            @RequestParam(required=false) Long actorUserUid,
            @RequestParam(required=false) AdminAuditActionType actionType,
            @RequestParam(required=false) AdminAuditTargetType targetType,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(service.auditLogs(actorUserUid,actionType,targetType,startDate,endDate,
                page,size,authentication));
    }
}
