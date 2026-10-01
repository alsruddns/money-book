package com.moneybook.backend.dashboard.controller;

import com.moneybook.backend.dashboard.dto.AccountSummaryResponse;
import com.moneybook.backend.dashboard.dto.CategorySummaryResponse;
import com.moneybook.backend.dashboard.dto.MonthlyDashboardResponse;
import com.moneybook.backend.dashboard.service.DashboardService;
import com.moneybook.backend.enums.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 월별 거래와 이체 흐름을 R 권한으로 조회하는 가계부 Dashboard API이다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    /** 거래 원장만 집계해 수입, 지출, 순수익과 거래 건수를 반환한다. */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyDashboardResponse> monthly(@PathVariable Long moneyBookUid,
                                                             @RequestParam @Min(1) @Max(9999) int year,
                                                             @RequestParam @Min(1) @Max(12) int month,
                                                             Authentication authentication) {
        return ResponseEntity.ok(service.monthly(moneyBookUid, year, month, authentication));
    }

    /** 지정한 거래 유형의 카테고리별 월 합계와 해당 유형 안에서의 비율을 반환한다. */
    @GetMapping("/categories")
    public ResponseEntity<List<CategorySummaryResponse>> categories(@PathVariable Long moneyBookUid,
                                                                      @RequestParam @Min(1) @Max(9999) int year,
                                                                      @RequestParam @Min(1) @Max(12) int month,
                                                                      @RequestParam TransactionType transactionType,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(service.categories(moneyBookUid, year, month, transactionType, authentication));
    }

    /** 계좌마다 거래 수입·지출과 별도 이체 유입·유출을 구분해 반환한다. */
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountSummaryResponse>> accounts(@PathVariable Long moneyBookUid,
                                                                   @RequestParam @Min(1) @Max(9999) int year,
                                                                   @RequestParam @Min(1) @Max(12) int month,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(service.accounts(moneyBookUid, year, month, authentication));
    }
}
