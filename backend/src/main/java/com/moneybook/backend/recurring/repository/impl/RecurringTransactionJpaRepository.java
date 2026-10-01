package com.moneybook.backend.recurring.repository.impl;

import com.moneybook.backend.entity.RecurringTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionJpaRepository extends JpaRepository<RecurringTransaction, Long> {
    @Query("""
            select rule from RecurringTransaction rule
            join fetch rule.category join fetch rule.account
            where rule.recurringTransactionUid = :ruleUid and rule.moneyBook.moneyBookUid = :bookUid
            """)
    Optional<RecurringTransaction> findDetail(@Param("ruleUid") Long ruleUid, @Param("bookUid") Long bookUid);

    /** Fetches display names for the entire rule list in one query. */
    @Query("""
            select rule from RecurringTransaction rule
            join fetch rule.category join fetch rule.account
            where rule.moneyBook.moneyBookUid = :bookUid
            order by rule.recurringTransactionUid asc
            """)
    List<RecurringTransaction> findForBook(@Param("bookUid") Long bookUid);

    @Query("""
            select rule.recurringTransactionUid from RecurringTransaction rule
            where rule.moneyBook.moneyBookUid = :bookUid and rule.active = true
              and rule.startDate <= :baseDate
            order by rule.recurringTransactionUid asc
            """)
    List<Long> findActiveIdsDueBy(@Param("bookUid") Long bookUid, @Param("baseDate") LocalDate baseDate);

    /** Serializes generation for a rule before reading its existing scheduled dates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rule from RecurringTransaction rule where rule.recurringTransactionUid = :ruleUid")
    Optional<RecurringTransaction> lockById(@Param("ruleUid") Long ruleUid);
}
