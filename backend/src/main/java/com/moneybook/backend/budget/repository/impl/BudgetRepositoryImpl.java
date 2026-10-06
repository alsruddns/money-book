package com.moneybook.backend.budget.repository.impl;

import com.moneybook.backend.budget.repository.BudgetRepository;
import com.moneybook.backend.entity.MoneyBookBudget;
import com.moneybook.backend.enums.TransactionType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BudgetRepositoryImpl implements BudgetRepository {
    private final EntityManager em;

    @Override
    public Optional<MoneyBookBudget> find(Long bookUid, int year, int month) {
        return em.createQuery("""
                select distinct b from MoneyBookBudget b
                left join fetch b.categories cb
                left join fetch cb.category
                where b.moneyBook.moneyBookUid = :bookUid and b.year = :year and b.month = :month
                """, MoneyBookBudget.class)
                .setParameter("bookUid", bookUid).setParameter("year", year).setParameter("month", month)
                .getResultStream().findFirst();
    }

    @Override
    public MoneyBookBudget save(MoneyBookBudget budget) {
        if (budget.getBudgetUid() == null) em.persist(budget);
        return budget;
    }

    /** Uses SQL SUM on expense transactions only; transfers and income never enter this table result. */
    @Override
    public BigDecimal totalExpense(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select coalesce(sum(t.amount), 0) from MoneyBookTransaction t
                where t.moneyBook.moneyBookUid = :bookUid and t.transactionType = :type
                  and t.transactionDate >= :start and t.transactionDate < :end
                """, BigDecimal.class)
                .setParameter("bookUid", bookUid).setParameter("type", TransactionType.EXPENSE)
                .setParameter("start", start).setParameter("end", endExclusive).getSingleResult();
    }

    /** One grouped query supplies all category expense amounts for the month. */
    @Override
    public List<CategoryExpense> categoryExpenses(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.budget.repository.BudgetRepository$CategoryExpense(
                    t.category.categoryUid, sum(t.amount))
                from MoneyBookTransaction t
                where t.moneyBook.moneyBookUid = :bookUid and t.transactionType = :type
                  and t.transactionDate >= :start and t.transactionDate < :end
                group by t.category.categoryUid
                """, CategoryExpense.class)
                .setParameter("bookUid", bookUid).setParameter("type", TransactionType.EXPENSE)
                .setParameter("start", start).setParameter("end", endExclusive).getResultList();
    }
}
