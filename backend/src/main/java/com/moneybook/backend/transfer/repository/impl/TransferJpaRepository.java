package com.moneybook.backend.transfer.repository.impl;

import com.moneybook.backend.entity.MoneyBookTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransferJpaRepository extends JpaRepository<MoneyBookTransfer, Long> {
    /** Fetches both account names for detail without row-by-row follow-up queries. */
    @Query("""
            select transfer from MoneyBookTransfer transfer
            join fetch transfer.fromAccount
            join fetch transfer.toAccount
            where transfer.transferUid = :transferUid and transfer.moneyBook.moneyBookUid = :bookUid
            """)
    Optional<MoneyBookTransfer> findDetail(@Param("transferUid") Long transferUid,
                                           @Param("bookUid") Long bookUid);

    /** Uses a half-open month range and joins both account names in one query. */
    @Query("""
            select transfer from MoneyBookTransfer transfer
            join fetch transfer.fromAccount
            join fetch transfer.toAccount
            where transfer.moneyBook.moneyBookUid = :bookUid
              and transfer.transferDate >= :from and transfer.transferDate < :until
            order by transfer.transferDate desc, transfer.transferUid desc
            """)
    List<MoneyBookTransfer> findForPeriod(@Param("bookUid") Long bookUid,
                                          @Param("from") LocalDate from,
                                          @Param("until") LocalDate until);
}
