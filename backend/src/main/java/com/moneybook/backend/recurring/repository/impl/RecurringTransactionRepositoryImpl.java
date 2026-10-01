package com.moneybook.backend.recurring.repository.impl;

import com.moneybook.backend.entity.RecurringTransaction;
import com.moneybook.backend.recurring.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecurringTransactionRepositoryImpl implements RecurringTransactionRepository {
    private final RecurringTransactionJpaRepository jpa;

    @Override
    public RecurringTransaction save(RecurringTransaction rule) {
        return jpa.saveAndFlush(rule);
    }

    @Override
    public Optional<RecurringTransaction> findByIdAndMoneyBookUid(Long ruleUid, Long moneyBookUid) {
        return jpa.findDetail(ruleUid, moneyBookUid);
    }

    @Override
    public List<RecurringTransaction> findByMoneyBookUid(Long moneyBookUid) {
        return jpa.findForBook(moneyBookUid);
    }

    @Override
    public List<Long> findActiveIdsDueBy(Long moneyBookUid, LocalDate baseDate) {
        return jpa.findActiveIdsDueBy(moneyBookUid, baseDate);
    }

    @Override
    public Optional<RecurringTransaction> lockById(Long ruleUid) {
        return jpa.lockById(ruleUid);
    }

    @Override
    public void delete(RecurringTransaction rule) {
        jpa.delete(rule);
    }
}
