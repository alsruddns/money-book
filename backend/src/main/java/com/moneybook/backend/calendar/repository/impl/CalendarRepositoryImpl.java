package com.moneybook.backend.calendar.repository.impl;

import com.moneybook.backend.calendar.repository.CalendarRepository;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.MoneyBookTransfer;
import com.moneybook.backend.enums.TransactionType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Monthly queries group in the database and daily detail queries fetch display names in one round trip. */
@Repository
@RequiredArgsConstructor
public class CalendarRepositoryImpl implements CalendarRepository {
    private final EntityManager em;

    @Override
    public List<TransactionDay> transactionDays(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.calendar.repository.CalendarRepository$TransactionDay(
                    t.transactionDate,
                    sum(case when t.transactionType = :income then t.amount else 0 end),
                    sum(case when t.transactionType = :expense then t.amount else 0 end),
                    count(t), count(t.recurringTransactionUid))
                from MoneyBookTransaction t
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transactionDate >= :start and t.transactionDate < :end
                group by t.transactionDate
                """, TransactionDay.class)
                .setParameter("bookUid", bookUid).setParameter("income", TransactionType.INCOME)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", start).setParameter("end", endExclusive).getResultList();
    }

    @Override
    public List<TransferDay> transferDays(Long bookUid, LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select new com.moneybook.backend.calendar.repository.CalendarRepository$TransferDay(
                    t.transferDate, sum(t.amount), count(t))
                from MoneyBookTransfer t
                where t.moneyBook.moneyBookUid = :bookUid
                  and t.transferDate >= :start and t.transferDate < :end
                group by t.transferDate
                """, TransferDay.class)
                .setParameter("bookUid", bookUid).setParameter("start", start)
                .setParameter("end", endExclusive).getResultList();
    }

    @Override
    public List<MoneyBookTransaction> transactionsOnDate(Long bookUid, LocalDate date) {
        return em.createQuery("""
                select t from MoneyBookTransaction t join fetch t.category join fetch t.account
                where t.moneyBook.moneyBookUid = :bookUid and t.transactionDate = :date
                order by t.transactionUid
                """, MoneyBookTransaction.class)
                .setParameter("bookUid", bookUid).setParameter("date", date).getResultList();
    }

    @Override
    public List<MoneyBookTransfer> transfersOnDate(Long bookUid, LocalDate date) {
        return em.createQuery("""
                select t from MoneyBookTransfer t join fetch t.fromAccount join fetch t.toAccount
                where t.moneyBook.moneyBookUid = :bookUid and t.transferDate = :date
                order by t.transferUid
                """, MoneyBookTransfer.class)
                .setParameter("bookUid", bookUid).setParameter("date", date).getResultList();
    }
}
