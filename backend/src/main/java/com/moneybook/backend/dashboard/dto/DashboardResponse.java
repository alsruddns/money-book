package com.moneybook.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Aggregated data needed by the selected-month dashboard view. */
public record DashboardResponse(
        int year,
        int month,
        Summary summary,
        Comparison comparison,
        List<CategoryExpense> categoryExpenses,
        List<MonthTrend> monthlyTrend,
        BudgetSummary budget,
        List<TopExpense> topExpenses
) {
    public record Summary(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance,
                          long transactionCount, long incomeCount, long expenseCount) { }
    public record Comparison(BigDecimal previousMonthIncome, BigDecimal previousMonthExpense,
                             BigDecimal incomeChangeRate, BigDecimal expenseChangeRate) { }
    /** ratio uses the same fraction convention as existing category statistics (0.34 means 34%). */
    public record CategoryExpense(Long categoryUid, String categoryName, BigDecimal amount,
                                 long transactionCount, BigDecimal ratio) { }
    public record MonthTrend(int year, int month, BigDecimal income, BigDecimal expense, BigDecimal balance) { }
    public record BudgetSummary(BigDecimal totalBudget, BigDecimal actualExpense, BigDecimal remaining,
                                BigDecimal usageRate, boolean overBudget) { }
    public record TopExpense(int rank, Long transactionUid, LocalDate transactionDate, Long categoryUid,
                             String categoryName, Long accountUid, String accountName,
                             String memo, BigDecimal amount) { }
}
