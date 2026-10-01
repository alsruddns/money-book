package com.moneybook.backend.account.repository.impl;

import com.moneybook.backend.account.repository.AccountRepository;
import com.moneybook.backend.entity.MoneyBookAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
    private final AccountJpaRepository jpa;

    @Override
    public MoneyBookAccount save(MoneyBookAccount account) {
        return jpa.saveAndFlush(account);
    }

    @Override
    public void flush() {
        jpa.flush();
    }

    @Override
    public Optional<MoneyBookAccount> findById(Long accountUid) {
        return jpa.findById(accountUid);
    }

    @Override
    public Optional<MoneyBookAccount> findByIdAndMoneyBookUid(Long accountUid, Long moneyBookUid) {
        return jpa.findByAccountUidAndMoneyBook_MoneyBookUid(accountUid, moneyBookUid);
    }

    @Override
    public List<MoneyBookAccount> findByMoneyBookUid(Long moneyBookUid) {
        return jpa.findByMoneyBook_MoneyBookUidOrderBySortOrderAscAccountUidAsc(moneyBookUid);
    }

    @Override
    public boolean existsByName(Long moneyBookUid, String name, Long exceptUid) {
        return jpa.existsDuplicate(moneyBookUid, name, exceptUid);
    }

    @Override
    public boolean isInUse(Long accountUid) {
        return jpa.isInUse(accountUid);
    }

    @Override
    public void delete(MoneyBookAccount account) {
        jpa.delete(account);
    }
}
