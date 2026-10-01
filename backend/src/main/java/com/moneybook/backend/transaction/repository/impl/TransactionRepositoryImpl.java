package com.moneybook.backend.transaction.repository.impl;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
    private final TransactionJpaRepository jpa;

    @Override
    public MoneyBookTransaction save(MoneyBookTransaction transaction) {
        return jpa.saveAndFlush(transaction);
    }

    @Override
    public Optional<MoneyBookTransaction> findByIdAndMoneyBookUid(Long transactionUid, Long moneyBookUid) {
        return jpa.findDetail(transactionUid, moneyBookUid);
    }

    @Override
    public List<MoneyBookTransaction> findForPeriod(Long moneyBookUid, LocalDate from, LocalDate until) {
        return jpa.findForPeriod(moneyBookUid, from, until);
    }

    @Override
    public void delete(MoneyBookTransaction transaction) {
        jpa.delete(transaction);
    }
}
