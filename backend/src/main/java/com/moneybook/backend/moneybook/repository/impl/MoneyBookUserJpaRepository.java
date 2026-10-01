package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Spring Data adapter used by MoneyBookUserRepositoryImpl. */
public interface MoneyBookUserJpaRepository extends JpaRepository<MoneyBookUser, Long> {

    /** Fetches each book with its qualifying membership to avoid N+1 queries. */
    @Query("""
            select membership from MoneyBookUser membership
            join fetch membership.moneyBook book
            where membership.userUid = :userUid
              and membership.invitationStatus = :status
              and membership.canRead = true
            order by book.moneyBookUid desc
            """)
    List<MoneyBookUser> findReadableByUserUidAndStatus(
            @Param("userUid") Long userUid, @Param("status") InvitationStatus status);

    Optional<MoneyBookUser> findByMoneyBook_MoneyBookUidAndUserUid(Long moneyBookUid, Long userUid);

    /** Fetches each pending invitation with its book so list mapping does not trigger N+1 queries. */
    @Query("""
            select membership from MoneyBookUser membership
            join fetch membership.moneyBook book
            where membership.userUid = :userUid
              and membership.invitationStatus = :status
            order by membership.moneyBookUserUid desc
            """)
    List<MoneyBookUser> findByUserUidAndInvitationStatusWithBook(
            @Param("userUid") Long userUid, @Param("status") InvitationStatus status);
}
