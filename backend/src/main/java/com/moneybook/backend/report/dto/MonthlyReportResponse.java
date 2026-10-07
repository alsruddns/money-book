package com.moneybook.backend.report.dto;

import java.math.BigDecimal;

/** Change rates are percentages; null means the previous amount was zero and growth is undefined. */
public record MonthlyReportResponse(int year, int month, BigDecimal income, BigDecimal expense,
                                    BigDecimal balance, long transactionCount,
                                    BigDecimal previousIncome, BigDecimal previousExpense,
                                    BigDecimal incomeChange, BigDecimal expenseChange,
                                    BigDecimal incomeChangeRate, BigDecimal expenseChangeRate,
                                    boolean budgetConfigured, BigDecimal totalBudget,
                                    BigDecimal remainingBudget, BigDecimal budgetUsageRate, boolean overBudget) { }
