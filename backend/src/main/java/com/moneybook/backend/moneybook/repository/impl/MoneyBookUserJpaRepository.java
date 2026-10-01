package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
