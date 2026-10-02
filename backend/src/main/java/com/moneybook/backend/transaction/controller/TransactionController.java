package com.moneybook.backend.transaction.controller;

import com.moneybook.backend.transaction.dto.CreateTransactionRequest;
import com.moneybook.backend.transaction.dto.TransactionResponse;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import com.moneybook.backend.transaction.dto.TransactionSearchResponse;
import com.moneybook.backend.transaction.dto.TransactionSearchSort;
import com.moneybook.backend.transaction.dto.UpdateTransactionRequest;
import com.moneybook.backend.transaction.service.TransactionService;
import com.moneybook.backend.transaction.service.TransactionSearchService;
import com.moneybook.backend.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

/** 가계부별 수입·지출 거래 원장 API를 제공하며 C/R/U/D 권한을 적용한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;
    private final TransactionSearchService searchService;

    /** C 권한으로 양수 금액의 수입 또는 지출 거래를 등록한다. */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@PathVariable Long moneyBookUid,
                                                      @Valid @RequestBody CreateTransactionRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(moneyBookUid, request, authentication));
    }

    /** R 권한으로 지정 월의 거래를 날짜와 UID 역순으로 조회하며 결과가 없으면 빈 배열을 반환한다. */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(@PathVariable Long moneyBookUid,
                                                           @RequestParam @Min(1) @Max(9999) int year,
                                                           @RequestParam @Min(1) @Max(12) int month,
                                                           Authentication authentication) {
        return ResponseEntity.ok(service.list(moneyBookUid, year, month, authentication));
    }

    /** R 권한으로 최대 2년의 거래를 필터·정렬·페이지 조건에 따라 검색한다. 날짜는 양 끝을 포함한다. */
    @GetMapping("/search")
    public TransactionSearchResponse search(@PathVariable Long moneyBookUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) Long categoryUid, @RequestParam(required = false) Long accountUid,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAmount, @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DATE_DESC") TransactionSearchSort sort, Authentication authentication) {
        return searchService.search(moneyBookUid, new TransactionSearchRequest(startDate, endDate,
                transactionType, categoryUid, accountUid, keyword, minAmount, maxAmount, page, size, sort),
                authentication);
    }

    /** R 권한으로 해당 가계부의 거래와 카테고리·계좌 이름을 조회한다. */
    @GetMapping("/{transactionUid}")
    public ResponseEntity<TransactionResponse> detail(@PathVariable Long moneyBookUid,
                                                      @PathVariable Long transactionUid,
                                                      Authentication authentication) {
        return ResponseEntity.ok(service.detail(moneyBookUid, transactionUid, authentication));
    }

    /** U 권한으로 거래를 수정하고 카테고리·계좌 소속과 유형을 다시 검증한다. */
    @PatchMapping("/{transactionUid}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long moneyBookUid,
                                                      @PathVariable Long transactionUid,
                                                      @Valid @RequestBody UpdateTransactionRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(service.update(moneyBookUid, transactionUid, request, authentication));
    }

    /** D 권한으로 해당 가계부의 거래를 삭제한다. */
    @DeleteMapping("/{transactionUid}")
    public ResponseEntity<Void> delete(@PathVariable Long moneyBookUid, @PathVariable Long transactionUid,
                                       Authentication authentication) {
        service.delete(moneyBookUid, transactionUid, authentication);
        return ResponseEntity.noContent().build();
    }
}
