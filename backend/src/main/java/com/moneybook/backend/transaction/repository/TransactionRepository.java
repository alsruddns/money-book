package com.moneybook.backend.transaction.repository;

import com.moneybook.backend.entity.MoneyBookTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    MoneyBookTransaction save(MoneyBookTransaction transaction);
    Optional<MoneyBookTransaction> findByIdAndMoneyBookUid(Long transactionUid, Long moneyBookUid);
    List<MoneyBookTransaction> findForPeriod(Long moneyBookUid, LocalDate from, LocalDate until);
    void delete(MoneyBookTransaction transaction);
}
