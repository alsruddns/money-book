package com.moneybook.backend.admin.controller;
import com.moneybook.backend.admin.dto.AdminPasswordResetResponse;
import com.moneybook.backend.admin.service.AdminPasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
/** SUPER_ADMIN만 LOCAL 사용자 비밀번호를 1회성 임시 비밀번호로 초기화한다. */
@RestController @RequestMapping("/admin/users") @RequiredArgsConstructor
public class AdminPasswordResetController {
 private final AdminPasswordResetService service;
 /** 임시 비밀번호 원문은 이 응답에서만 반환하며, SUPER_ADMIN 계정 대상 초기화는 거부한다. */
 @PostMapping("/{userUid}/password-reset") public ResponseEntity<AdminPasswordResetResponse> reset(@PathVariable Long userUid,Authentication authentication){return ResponseEntity.ok(service.reset(userUid,authentication));}
}
