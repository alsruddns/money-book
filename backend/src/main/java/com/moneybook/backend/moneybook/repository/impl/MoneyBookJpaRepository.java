package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBook;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Spring Data adapter used by MoneyBookRepositoryImpl. */
public interface MoneyBookJpaRepository extends JpaRepository<MoneyBook, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select book from MoneyBook book where book.moneyBookUid = :moneyBookUid")
    Optional<MoneyBook> findForUpdate(@Param("moneyBookUid") Long moneyBookUid);

    long countByOwnerUserUid(Long ownerUserUid);
}
