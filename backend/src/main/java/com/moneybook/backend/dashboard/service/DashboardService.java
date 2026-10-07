package com.moneybook.backend.dashboard.service;

import com.moneybook.backend.dashboard.dto.AccountSummaryResponse;
import com.moneybook.backend.dashboard.dto.CategorySummaryResponse;
import com.moneybook.backend.dashboard.dto.MonthlyDashboardResponse;
import com.moneybook.backend.dashboard.dto.DashboardResponse;
import com.moneybook.backend.enums.TransactionType;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface DashboardService {
    DashboardResponse dashboard(Long bookUid, int year, int month, Authentication authentication);
    MonthlyDashboardResponse monthly(Long bookUid, int year, int month, Authentication authentication);
    List<CategorySummaryResponse> categories(Long bookUid, int year, int month,
                                             TransactionType type, Authentication authentication);
    List<AccountSummaryResponse> accounts(Long bookUid, int year, int month, Authentication authentication);
}
