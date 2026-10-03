package com.moneybook.backend.admin.controller;

import com.moneybook.backend.admin.dto.*;
import com.moneybook.backend.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 금융 payload를 노출하지 않는 가계부 운영 목록 및 상세 조회 API. */
@RestController
@RequestMapping("/admin/money-books")
@RequiredArgsConstructor
public class AdminMoneyBookController {
    private final AdminService service;

    /** 가계부 이름 또는 owner UID로 최소 운영 목록을 조회한다. */
    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminMoneyBookResponse>> moneyBooks(
            @RequestParam(required=false) String keyword, @RequestParam(required=false) Long ownerUserUid,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(service.moneyBooks(keyword,ownerUserUid,page,size,authentication));
    }

    /** 거래 내용이나 memo 없이 가계부 운영 aggregate를 조회하고 Admin Audit을 남긴다. */
    @GetMapping("/{moneyBookUid}")
    public ResponseEntity<AdminMoneyBookDetailResponse> moneyBook(@PathVariable Long moneyBookUid,
            Authentication authentication) {
        return ResponseEntity.ok(service.moneyBook(moneyBookUid,authentication));
    }

    /** Lists a book's member and invitation permission state for operational inspection only. */
    @GetMapping("/{moneyBookUid}/members")
    public ResponseEntity<AdminPageResponse<AdminMoneyBookMemberResponse>> members(
            @PathVariable Long moneyBookUid, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size, Authentication authentication) {
        return ResponseEntity.ok(service.moneyBookMembers(moneyBookUid, page, size, authentication));
    }
}
