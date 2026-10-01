package com.moneybook.backend.moneybook.repository;

import com.moneybook.backend.entity.MoneyBookUser;

import java.util.List;

public interface MoneyBookUserRepository {

    MoneyBookUser save(MoneyBookUser membership);

    List<MoneyBookUser> findReadableAcceptedByUserUid(Long userUid);
}
