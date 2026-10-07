package com.moneybook.backend.closing.dto;

import com.moneybook.backend.entity.MoneyBookMonthClosing;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Stable values captured when the month was closed. */
public record MonthClosingResponse(Long closingUid, Long moneyBookUid, int year, int month,
                                   BigDecimal income, BigDecimal expense, BigDecimal balance,
                                   long transactionCount, BigDecimal previousIncome, BigDecimal previousExpense,
                                   BigDecimal incomeChange, BigDecimal expenseChange,
                                   BigDecimal incomeChangeRate, BigDecimal expenseChangeRate,
                                   boolean budgetConfigured, BigDecimal totalBudget,
                                   BigDecimal remainingBudget, BigDecimal budgetUsageRate, boolean overBudget,
                                   Long closedByUserUid, LocalDateTime closedAt) {
    public static MonthClosingResponse from(MoneyBookMonthClosing closing) {
        BigDecimal income = closing.getIncome();
        BigDecimal expense = closing.getExpense();
        BigDecimal limit = closing.getTotalBudget();
        BigDecimal incomeChange = income.subtract(closing.getPreviousIncome());
        BigDecimal expenseChange = expense.subtract(closing.getPreviousExpense());
        return new MonthClosingResponse(closing.getClosingUid(), closing.getMoneyBook().getMoneyBookUid(),
                closing.getYear(), closing.getMonth(), income, expense, income.subtract(expense),
                closing.getTransactionCount(), closing.getPreviousIncome(), closing.getPreviousExpense(),
                incomeChange, expenseChange, rate(incomeChange, closing.getPreviousIncome()),
                rate(expenseChange, closing.getPreviousExpense()), closing.isBudgetConfigured(), limit,
                limit == null ? null : limit.subtract(expense),
                limit == null ? null : limit.signum() == 0 ? BigDecimal.ZERO
                        : expense.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP),
                limit != null && expense.compareTo(limit) > 0,
                closing.getClosedByUserUid(), closing.getClosedAt());
    }

    private static BigDecimal rate(BigDecimal change, BigDecimal previous) {
        if (previous.signum() == 0) return change.signum() == 0 ? BigDecimal.ZERO : null;
        return change.multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }
}
