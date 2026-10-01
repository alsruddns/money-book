package com.moneybook.backend.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyDashboardResponse(int year, int month, BigDecimal totalIncome,
                                       BigDecimal totalExpense, BigDecimal balance,
                                       long transactionCount, long incomeCount, long expenseCount) { }
