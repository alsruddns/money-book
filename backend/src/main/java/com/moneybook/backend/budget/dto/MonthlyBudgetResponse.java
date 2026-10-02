package com.moneybook.backend.budget.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyBudgetResponse(boolean configured, Long budgetUid, Long moneyBookUid, int year, int month,
                                    BigDecimal totalBudget, BigDecimal totalExpense, BigDecimal remainingBudget,
                                    BigDecimal usageRate, boolean overBudget,
                                    List<CategoryBudgetResponse> categories) { }
