package com.moneybook.backend.report.controller;

import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.report.dto.AccountStatisticsResponse;
import com.moneybook.backend.report.dto.CategoryStatisticsResponse;
import com.moneybook.backend.report.dto.MonthlyReportResponse;
import com.moneybook.backend.report.dto.YearlyReportResponse;
import com.moneybook.backend.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** R 권한으로 가계부 거래의 연간 추이, 기간 통계와 월간 실적을 제공한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService service;

    /** 거래가 없는 월도 0으로 채운 12개월 수입·지출 추이를 조회한다. */
    @GetMapping("/yearly")
    public YearlyReportResponse yearly(@PathVariable Long moneyBookUid, @RequestParam int year,
                                       Authentication authentication) {
        return service.yearly(moneyBookUid, year, authentication);
    }

    /** 지정 기간과 거래 유형의 카테고리 금액·건수·비율을 조회한다. 기간 양 끝을 포함한다. */
    @GetMapping("/categories")
    public List<CategoryStatisticsResponse> categories(@PathVariable Long moneyBookUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam TransactionType transactionType, Authentication authentication) {
        return service.categories(moneyBookUid, startDate, endDate, transactionType, authentication);
    }

    /** 지정 기간의 계좌별 수입·지출·이체와 순변동을 조회한다. */
    @GetMapping("/accounts")
    public List<AccountStatisticsResponse> accounts(@PathVariable Long moneyBookUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        return service.accounts(moneyBookUid, startDate, endDate, authentication);
    }

    /** 해당 월 실적과 전월 대비 증감, 설정된 총예산 대비 지출을 조회한다. */
    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(@PathVariable Long moneyBookUid, @RequestParam int year,
                                         @RequestParam int month, Authentication authentication) {
        return service.monthly(moneyBookUid, year, month, authentication);
    }
}
