package com.moneybook.backend.accountmanagement.controller;

import com.moneybook.backend.accountmanagement.dto.AccountMeResDto;
import com.moneybook.backend.accountmanagement.dto.AccountPasswordUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountProfileUpdateReqDto;
import com.moneybook.backend.accountmanagement.dto.AccountWithdrawalRequest;
import com.moneybook.backend.accountmanagement.service.AccountManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자가 자신의 계정 정보를 조회하고 관리하는 API를 제공한다. */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountManagementController {

    private final AccountManagementService accountManagementService;

    /** 인증된 사용자의 계정 관리 정보를 반환하며 인증 정보는 응답에 포함하지 않는다. */
    @GetMapping("/me")
    public ResponseEntity<AccountMeResDto> me(Authentication authentication) {
        return ResponseEntity.ok(accountManagementService.me(authentication));
    }

    /** 인증된 사용자의 닉네임을 변경하고 변경 후 최신 계정 정보를 반환한다. */
    @PatchMapping("/profile")
    public ResponseEntity<AccountMeResDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody AccountProfileUpdateReqDto request) {
        return ResponseEntity.ok(accountManagementService.updateProfile(authentication, request));
    }

    /** LOCAL 계정의 현재 비밀번호를 확인한 뒤 새 BCrypt 비밀번호를 저장한다. */
    @PatchMapping("/password")
    public ResponseEntity<AccountMeResDto> updatePassword(
            Authentication authentication,
            @Valid @RequestBody AccountPasswordUpdateReqDto request) {
        return ResponseEntity.ok(accountManagementService.updatePassword(authentication, request));
    }

    /** LOCAL 계정은 비밀번호를 재확인하고, 소유 중인 가계부가 없을 때 현재 계정을 탈퇴 처리한다. */
    @DeleteMapping
    public ResponseEntity<Void> withdraw(Authentication authentication,
                                         @Valid @RequestBody AccountWithdrawalRequest request) {
        accountManagementService.withdraw(authentication, request);
        return ResponseEntity.noContent().build();
    }
}
