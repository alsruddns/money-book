package com.moneybook.backend.report.service;

import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.report.dto.AccountStatisticsResponse;
import com.moneybook.backend.report.dto.CategoryStatisticsResponse;
import com.moneybook.backend.report.dto.MonthlyReportResponse;
import com.moneybook.backend.report.dto.YearlyReportResponse;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    YearlyReportResponse yearly(Long bookUid, int year, Authentication authentication);
    List<CategoryStatisticsResponse> categories(Long bookUid, LocalDate start, LocalDate end,
                                                TransactionType type, Authentication authentication);
    List<AccountStatisticsResponse> accounts(Long bookUid, LocalDate start, LocalDate end,
                                             Authentication authentication);
    MonthlyReportResponse monthly(Long bookUid, int year, int month, Authentication authentication);
}
