package com.moneybook.backend.moneybook.repository;

import com.moneybook.backend.entity.MoneyBookUser;

import java.util.List;
import java.util.Optional;

public interface MoneyBookUserRepository {

    MoneyBookUser save(MoneyBookUser membership);

    List<MoneyBookUser> findReadableAcceptedByUserUid(Long userUid);

    Optional<MoneyBookUser> findByMoneyBookUidAndUserUid(Long moneyBookUid, Long userUid);

    Optional<MoneyBookUser> findById(Long moneyBookUserUid);

    List<MoneyBookUser> findPendingByUserUid(Long userUid);
}
