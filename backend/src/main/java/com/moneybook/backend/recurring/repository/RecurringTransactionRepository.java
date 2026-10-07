package com.moneybook.backend.recurring.repository;

import com.moneybook.backend.entity.RecurringTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository {
    RecurringTransaction save(RecurringTransaction rule);
    Optional<RecurringTransaction> findByIdAndMoneyBookUid(Long ruleUid, Long moneyBookUid);
    List<RecurringTransaction> findByMoneyBookUid(Long moneyBookUid);
    List<Long> findActiveIdsDueBy(Long moneyBookUid, LocalDate baseDate);
    Optional<RecurringTransaction> lockById(Long ruleUid);
    void delete(RecurringTransaction rule);
}
