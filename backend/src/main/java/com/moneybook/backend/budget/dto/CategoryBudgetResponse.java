package com.moneybook.backend.budget.dto;

import java.math.BigDecimal;

public record CategoryBudgetResponse(Long categoryUid, String categoryName, BigDecimal budgetAmount,
                                     BigDecimal expenseAmount, BigDecimal remainingAmount,
                                     BigDecimal usageRate, boolean overBudget) { }
