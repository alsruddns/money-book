package com.moneybook.backend.budget.service;

import com.moneybook.backend.budget.dto.MonthlyBudgetResponse;
import com.moneybook.backend.budget.dto.SaveBudgetRequest;
import org.springframework.security.core.Authentication;

public interface BudgetService {
    MonthlyBudgetResponse get(Long bookUid, int year, int month, Authentication authentication);
    MonthlyBudgetResponse save(Long bookUid, int year, int month, SaveBudgetRequest request,
                               Authentication authentication);
}
