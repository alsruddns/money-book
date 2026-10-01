package com.moneybook.backend.transaction.repository.impl;

import com.moneybook.backend.entity.MoneyBookTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionJpaRepository extends JpaRepository<MoneyBookTransaction, Long> {

    /** Loads both display names with the transaction, including the detail endpoint. */
    @Query("""
            select entry from MoneyBookTransaction entry
            join fetch entry.category
            join fetch entry.account
            where entry.transactionUid = :transactionUid and entry.moneyBook.moneyBookUid = :bookUid
            """)
    Optional<MoneyBookTransaction> findDetail(@Param("transactionUid") Long transactionUid,
                                               @Param("bookUid") Long bookUid);

    /** Half-open month range with one join query; no row-by-row name lookups. */
    @Query("""
            select entry from MoneyBookTransaction entry
            join fetch entry.category
            join fetch entry.account
            where entry.moneyBook.moneyBookUid = :bookUid
              and entry.transactionDate >= :from and entry.transactionDate < :until
            order by entry.transactionDate desc, entry.transactionUid desc
            """)
    List<MoneyBookTransaction> findForPeriod(@Param("bookUid") Long bookUid,
                                              @Param("from") LocalDate from,
                                              @Param("until") LocalDate until);

}
