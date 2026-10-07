package com.moneybook.backend.budget.repository;

import com.moneybook.backend.entity.MoneyBookBudget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository {
    Optional<MoneyBookBudget> find(Long bookUid, int year, int month);
    MoneyBookBudget save(MoneyBookBudget budget);
    BigDecimal totalExpense(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<CategoryExpense> categoryExpenses(Long bookUid, LocalDate start, LocalDate endExclusive);

    record CategoryExpense(Long categoryUid, BigDecimal amount) { }
}
