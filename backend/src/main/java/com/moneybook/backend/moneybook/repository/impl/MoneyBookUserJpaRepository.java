package com.moneybook.backend.moneybook.repository.impl;

import com.moneybook.backend.entity.MoneyBookUser;
import com.moneybook.backend.enums.InvitationStatus;
import com.moneybook.backend.moneybook.repository.MoneyBookMemberRow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select membership from MoneyBookUser membership where membership.moneyBook.moneyBookUid = :bookUid and membership.userUid = :userUid")
    Optional<MoneyBookUser> findForUpdate(@Param("bookUid") Long bookUid, @Param("userUid") Long userUid);

    @Modifying
    @Query("delete from MoneyBookUser membership where membership.userUid = :userUid")
    int deleteAllForUser(@Param("userUid") Long userUid);

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

    /** Joins users once for nicknames and orders the book owner before other accepted members. */
    @Query("""
            select new com.moneybook.backend.moneybook.repository.MoneyBookMemberRow(
                membership.moneyBookUserUid, book.moneyBookUid, membership.userUid,
                member.nickname, book.ownerUserUid, membership.admin,
                membership.canCreate, membership.canRead, membership.canUpdate, membership.canDelete)
            from MoneyBookUser membership
            join membership.moneyBook book
            join User member on member.userUid = membership.userUid
            where book.moneyBookUid = :moneyBookUid
              and membership.invitationStatus = :status
            order by case when membership.userUid = book.ownerUserUid then 0 else 1 end,
                     membership.moneyBookUserUid asc
            """)
    List<MoneyBookMemberRow> findMembersByBookUidAndStatus(
            @Param("moneyBookUid") Long moneyBookUid, @Param("status") InvitationStatus status);
}
