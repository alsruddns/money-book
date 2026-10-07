package com.moneybook.backend.recurring.controller;

import com.moneybook.backend.recurring.dto.CreateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionRequest;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionResponse;
import com.moneybook.backend.recurring.dto.RecurringTransactionResponse;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionActiveRequest;
import com.moneybook.backend.recurring.dto.UpdateRecurringTransactionRequest;
import com.moneybook.backend.recurring.service.RecurringTransactionService;
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

/** 가계부의 정기 수입·지출 규칙과 명시적 실제 거래 생성 API를 제공한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {
    private final RecurringTransactionService service;

    /** C 권한으로 월별 또는 주별 정기 규칙을 등록한다. */
    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(
            @PathVariable Long moneyBookUid, @Valid @RequestBody CreateRecurringTransactionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(moneyBookUid, request, authentication));
    }

    /** R 권한으로 카테고리·계좌 이름과 상태를 포함한 규칙 목록을 조회한다. */
    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> list(
            @PathVariable Long moneyBookUid, Authentication authentication) {
        return ResponseEntity.ok(service.list(moneyBookUid, authentication));
    }

    /** U 권한으로 규칙 전체를 다시 검증한 뒤 수정한다. */
    @PatchMapping("/{recurringTransactionUid}")
    public ResponseEntity<RecurringTransactionResponse> update(
            @PathVariable Long moneyBookUid, @PathVariable Long recurringTransactionUid,
            @Valid @RequestBody UpdateRecurringTransactionRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.update(moneyBookUid, recurringTransactionUid, request, authentication));
    }

    /** U 권한으로 생성 여부만 명시적으로 켜거나 끈다. */
    @PatchMapping("/{recurringTransactionUid}/active")
    public ResponseEntity<RecurringTransactionResponse> changeActive(
            @PathVariable Long moneyBookUid, @PathVariable Long recurringTransactionUid,
            @Valid @RequestBody UpdateRecurringTransactionActiveRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.changeActive(moneyBookUid, recurringTransactionUid, request, authentication));
    }

    /** D 권한으로 규칙만 삭제한다. 이미 생성된 실제 거래는 유지한다. */
    @DeleteMapping("/{recurringTransactionUid}")
    public ResponseEntity<Void> delete(@PathVariable Long moneyBookUid,
                                       @PathVariable Long recurringTransactionUid,
                                       Authentication authentication) {
        service.delete(moneyBookUid, recurringTransactionUid, authentication);
        return ResponseEntity.noContent().build();
    }

    /** C 권한으로 baseDate까지 도래한 누락 거래를 중복 없이 한 트랜잭션에서 생성한다. */
    @PostMapping("/generate")
    public ResponseEntity<GenerateRecurringTransactionResponse> generate(
            @PathVariable Long moneyBookUid, @Valid @RequestBody GenerateRecurringTransactionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.generate(moneyBookUid, request, authentication));
    }
}
