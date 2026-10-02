package com.moneybook.backend.report.repository.impl;

import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.report.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final EntityManager em;

    /** One grouped aggregate returns all active months; the service fills empty months. */
    @Override
    public List<MonthTotal> monthlyTotals(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.report.repository.ReportRepository$MonthTotal(
                    month(t.transactionDate),
                    sum(case when t.transactionType = :income then t.amount else 0 end),
                    sum(case when t.transactionType = :expense then t.amount else 0 end), count(t))
                from MoneyBookTransaction t
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transactionDate >= :start and t.transactionDate < :end
                group by month(t.transactionDate)
                """, MonthTotal.class)
                .setParameter("bookUid", bookUid).setParameter("income", TransactionType.INCOME)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", start).setParameter("end", endExclusive).getResultList();
    }

    /** Reads at most the requested expense rows and resolves category/account labels in one query. */
    @Override
    public List<ExpenseRow> expenseRanking(Long bookUid, LocalDate start, LocalDate endExclusive, int limit) {
        return em.createQuery("""
                select new com.moneybook.backend.report.repository.ReportRepository$ExpenseRow(
                    t.transactionUid, t.transactionDate, c.categoryUid, c.name,
                    a.accountUid, a.name, t.memo, t.amount)
                from MoneyBookTransaction t join t.category c join t.account a
                where t.moneyBook.moneyBookUid = :bookUid and t.transactionType = :type
                  and t.transactionDate >= :start and t.transactionDate < :end
                order by t.amount desc, t.transactionDate desc, t.transactionUid desc
                """, ExpenseRow.class)
                .setParameter("bookUid", bookUid).setParameter("type", TransactionType.EXPENSE)
                .setParameter("start", start).setParameter("end", endExclusive)
                .setMaxResults(limit).getResultList();
    }
}
