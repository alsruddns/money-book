package com.moneybook.backend.report.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReportRepository {
    List<MonthTotal> monthlyTotals(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<ExpenseRow> expenseRanking(Long bookUid, LocalDate start, LocalDate endExclusive, int limit);

    record MonthTotal(int month, BigDecimal income, BigDecimal expense, long count) { }
    record ExpenseRow(Long transactionUid, LocalDate transactionDate, Long categoryUid, String categoryName,
                      Long accountUid, String accountName, String memo, BigDecimal amount) { }
}
