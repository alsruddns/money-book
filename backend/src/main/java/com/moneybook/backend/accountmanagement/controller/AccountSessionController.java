package com.moneybook.backend.accountmanagement.controller;

import com.moneybook.backend.accountmanagement.dto.RefreshSessionResponse;
import com.moneybook.backend.accountmanagement.service.AccountSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 인증된 사용자의 Refresh Session 목록과 폐기 API를 제공한다. */
@RestController
@RequestMapping("/account/sessions")
@RequiredArgsConstructor
public class AccountSessionController {
    private final AccountSessionService service;

    /** 현재 사용자의 만료되지 않은 세션 메타데이터를 반환하고 현재 세션을 표시한다. */
    @GetMapping
    public ResponseEntity<List<RefreshSessionResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(service.list(authentication));
    }

    /** 현재 사용자가 소유한 지정 세션 하나를 폐기한다. */
    @DeleteMapping("/{sessionUid}")
    public ResponseEntity<Void> revoke(Authentication authentication, @PathVariable Long sessionUid) {
        service.revoke(authentication, sessionUid);
        return ResponseEntity.noContent().build();
    }

    /** 현재 세션을 포함해 사용자의 모든 Refresh Session을 폐기한다. */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> revokeAll(Authentication authentication) {
        service.revokeAll(authentication);
        return ResponseEntity.noContent().build();
    }
}
