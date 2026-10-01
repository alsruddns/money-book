package com.moneybook.backend.moneybook.repository;

import com.moneybook.backend.entity.MoneyBook;

import java.util.Optional;

public interface MoneyBookRepository {

    MoneyBook save(MoneyBook moneyBook);

    Optional<MoneyBook> findById(Long moneyBookUid);
}
