package com.moneybook.backend.dashboard.repository.impl;

import com.moneybook.backend.dashboard.repository.DashboardRepository;
import com.moneybook.backend.enums.TransactionType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Groups monthly data in the database; no transaction or transfer entity list is loaded. */
@Repository
@RequiredArgsConstructor
public class DashboardRepositoryImpl implements DashboardRepository {
    private final EntityManager em;

    @Override
    public List<TypeTotal> transactionTotals(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.dashboard.repository.DashboardRepository$TypeTotal(
                    t.transactionType, sum(t.amount), count(t))
                from MoneyBookTransaction t
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transactionDate >= :start and t.transactionDate < :end
                group by t.transactionType
                """, TypeTotal.class)
                .setParameter("bookUid", bookUid).setParameter("start", start)
                .setParameter("end", endExclusive).getResultList();
    }

    @Override
    public List<CategoryTotal> categoryTotals(Long bookUid, LocalDate start, LocalDate endExclusive,
                                               TransactionType type) {
        return em.createQuery("""
                select new com.moneybook.backend.dashboard.repository.DashboardRepository$CategoryTotal(
                    c.categoryUid, c.name, t.transactionType, sum(t.amount), count(t))
                from MoneyBookTransaction t join t.category c
                where t.moneyBook.moneyBookUid = :bookUid and t.transactionType = :type
                  and t.transactionDate >= :start and t.transactionDate < :end
                group by c.categoryUid, c.name, t.transactionType
                order by sum(t.amount) desc, c.categoryUid asc
                """, CategoryTotal.class)
                .setParameter("bookUid", bookUid).setParameter("type", type)
                .setParameter("start", start).setParameter("end", endExclusive).getResultList();
    }

    @Override
    public List<AccountTransactionTotal> accountTransactionTotals(Long bookUid, LocalDate start,
                                                                   LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.dashboard.repository.DashboardRepository$AccountTransactionTotal(
                    a.accountUid, t.transactionType, sum(t.amount))
                from MoneyBookTransaction t join t.account a
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transactionDate >= :start and t.transactionDate < :end
                group by a.accountUid, t.transactionType
                """, AccountTransactionTotal.class)
                .setParameter("bookUid", bookUid).setParameter("start", start)
                .setParameter("end", endExclusive).getResultList();
    }

    @Override
    public List<AccountTransferTotal> transferInTotals(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.dashboard.repository.DashboardRepository$AccountTransferTotal(
                    a.accountUid, sum(t.amount))
                from MoneyBookTransfer t join t.toAccount a
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transferDate >= :start and t.transferDate < :end
                group by a.accountUid
                """, AccountTransferTotal.class)
                .setParameter("bookUid", bookUid).setParameter("start", start)
                .setParameter("end", endExclusive).getResultList();
    }

    @Override
    public List<AccountTransferTotal> transferOutTotals(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.dashboard.repository.DashboardRepository$AccountTransferTotal(
                    a.accountUid, sum(t.amount))
                from MoneyBookTransfer t join t.fromAccount a
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transferDate >= :start and t.transferDate < :end
                group by a.accountUid
                """, AccountTransferTotal.class)
                .setParameter("bookUid", bookUid).setParameter("start", start)
                .setParameter("end", endExclusive).getResultList();
    }
}
