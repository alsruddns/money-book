package com.moneybook.backend.account.controller;

import com.moneybook.backend.account.dto.AccountResponse;
import com.moneybook.backend.account.dto.CreateAccountRequest;
import com.moneybook.backend.account.dto.UpdateAccountRequest;
import com.moneybook.backend.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 가계부별 계좌 및 결제수단 API를 제공한다. 모든 요청에 가계부 권한을 적용한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService service;

    /** C 권한으로 계좌 이름, 유형 및 정렬 순서를 등록한다. */
    @PostMapping
    public ResponseEntity<AccountResponse> create(@PathVariable Long moneyBookUid,
                                                   @Valid @RequestBody CreateAccountRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(moneyBookUid, request, authentication));
    }

    /** R 권한으로 정렬 순서에 따른 계좌 목록을 조회한다. */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(@PathVariable Long moneyBookUid,
                                                       Authentication authentication) {
        return ResponseEntity.ok(service.list(moneyBookUid, authentication));
    }

    /** U 권한으로 계좌의 이름, 유형 및 정렬 순서를 수정한다. */
    @PatchMapping("/{accountUid}")
    public ResponseEntity<AccountResponse> update(@PathVariable Long moneyBookUid, @PathVariable Long accountUid,
                                                   @Valid @RequestBody UpdateAccountRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(service.update(moneyBookUid, accountUid, request, authentication));
    }

    /** D 권한으로 거래에서 사용하지 않는 계좌만 삭제한다. */
    @DeleteMapping("/{accountUid}")
    public ResponseEntity<Void> delete(@PathVariable Long moneyBookUid, @PathVariable Long accountUid,
                                       Authentication authentication) {
        service.delete(moneyBookUid, accountUid, authentication);
        return ResponseEntity.noContent().build();
    }
}
