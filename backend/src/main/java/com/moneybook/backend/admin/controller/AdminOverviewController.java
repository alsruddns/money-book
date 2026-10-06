package com.moneybook.backend.admin.controller;

import com.moneybook.backend.admin.dto.AdminMeResponse;
import com.moneybook.backend.admin.dto.AdminOverviewResponse;
import com.moneybook.backend.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 관리자 화면의 세션 정보와 비민감 운영 통계를 제공한다. */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOverviewController {
    private final AdminService service;

    /** 현재 DB 기준 관리자 UID, nickname, systemRole을 조회한다. */
    @GetMapping("/me")
    public ResponseEntity<AdminMeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(service.me(authentication));
    }

    /** 금융 집계를 포함하지 않는 서비스 운영 요약 통계를 조회한다. */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> overview(Authentication authentication) {
        return ResponseEntity.ok(service.overview(authentication));
    }
}
