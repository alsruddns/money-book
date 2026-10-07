package com.moneybook.backend.account.repository.impl;

import com.moneybook.backend.entity.MoneyBookAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<MoneyBookAccount, Long> {
    Optional<MoneyBookAccount> findByAccountUidAndMoneyBook_MoneyBookUid(Long accountUid, Long moneyBookUid);

    List<MoneyBookAccount> findByMoneyBook_MoneyBookUidOrderBySortOrderAscAccountUidAsc(Long moneyBookUid);

    @Query("""
            select (count(account) > 0) from MoneyBookAccount account
            where account.moneyBook.moneyBookUid = :bookUid and account.name = :name
              and (:exceptUid is null or account.accountUid <> :exceptUid)
            """)
    boolean existsDuplicate(@Param("bookUid") Long bookUid, @Param("name") String name,
                            @Param("exceptUid") Long exceptUid);

    @Query("select (count(entry) > 0) from MoneyBookTransaction entry where entry.account.accountUid = :accountUid")
    boolean isInUse(@Param("accountUid") Long accountUid);
}
