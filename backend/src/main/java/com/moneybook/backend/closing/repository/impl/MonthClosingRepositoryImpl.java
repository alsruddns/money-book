package com.moneybook.backend.closing.repository.impl;

import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookMonthClosing;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.YearMonth;

@Repository
@RequiredArgsConstructor
public class MonthClosingRepositoryImpl implements MonthClosingRepository {
    private final EntityManager em;

    /** Serializes monthly closing with ledger and budget writes on the same book. */
    @Override
    public void lockBook(Long bookUid) {
        MoneyBook book = em.find(MoneyBook.class, bookUid);
        em.lock(book, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public boolean exists(Long bookUid, int year, int month) {
        return em.createQuery("""
                select count(c) from MoneyBookMonthClosing c
                where c.moneyBook.moneyBookUid = :bookUid and c.year = :year and c.month = :month
                """, Long.class).setParameter("bookUid", bookUid).setParameter("year", year)
                .setParameter("month", month).getSingleResult() > 0;
    }

    /** One query checks all candidate months for bulk recurring generation. */
    @Override
    public List<YearMonth> findClosedYears(Long bookUid, int firstYear, int lastYear) {
        return em.createQuery("""
                select c.year, c.month from MoneyBookMonthClosing c
                where c.moneyBook.moneyBookUid = :bookUid and c.year between :firstYear and :lastYear
                """, Object[].class).setParameter("bookUid", bookUid).setParameter("firstYear", firstYear)
                .setParameter("lastYear", lastYear).getResultList().stream()
                .map(row -> YearMonth.of((Integer) row[0], (Integer) row[1])).toList();
    }

    @Override
    public Optional<MoneyBookMonthClosing> find(Long bookUid, int year, int month) {
        return em.createQuery("""
                select c from MoneyBookMonthClosing c
                where c.moneyBook.moneyBookUid = :bookUid and c.year = :year and c.month = :month
                """, MoneyBookMonthClosing.class).setParameter("bookUid", bookUid).setParameter("year", year)
                .setParameter("month", month).getResultStream().findFirst();
    }

    @Override
    public MoneyBookMonthClosing save(MoneyBookMonthClosing closing) {
        em.persist(closing);
        return closing;
    }

    @Override
    public void delete(MoneyBookMonthClosing closing) {
        em.remove(closing);
    }
}
