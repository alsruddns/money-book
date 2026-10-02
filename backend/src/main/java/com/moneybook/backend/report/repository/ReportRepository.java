package com.moneybook.backend.report.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReportRepository {
    List<MonthTotal> monthlyTotals(Long bookUid, LocalDate start, LocalDate endExclusive);

    record MonthTotal(int month, BigDecimal income, BigDecimal expense, long count) { }
}
