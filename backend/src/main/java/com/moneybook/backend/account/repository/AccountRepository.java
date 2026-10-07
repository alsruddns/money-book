package com.moneybook.backend.account.repository;

import com.moneybook.backend.entity.MoneyBookAccount;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    MoneyBookAccount save(MoneyBookAccount account);
    void flush();
    Optional<MoneyBookAccount> findById(Long accountUid);
    Optional<MoneyBookAccount> findByIdAndMoneyBookUid(Long accountUid, Long moneyBookUid);
    List<MoneyBookAccount> findByMoneyBookUid(Long moneyBookUid);
    boolean existsByName(Long moneyBookUid, String name, Long exceptAccountUid);
    boolean isInUse(Long accountUid);
    void delete(MoneyBookAccount account);
}
