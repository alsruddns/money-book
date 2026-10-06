package com.moneybook.backend.transfer.repository.impl;

import com.moneybook.backend.entity.MoneyBookTransfer;
import com.moneybook.backend.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransferRepositoryImpl implements TransferRepository {
    private final TransferJpaRepository jpa;

    @Override
    public MoneyBookTransfer save(MoneyBookTransfer transfer) {
        return jpa.saveAndFlush(transfer);
    }

    @Override
    public Optional<MoneyBookTransfer> findByIdAndMoneyBookUid(Long transferUid, Long moneyBookUid) {
        return jpa.findDetail(transferUid, moneyBookUid);
    }

    @Override
    public List<MoneyBookTransfer> findForPeriod(Long moneyBookUid, LocalDate from, LocalDate until) {
        return jpa.findForPeriod(moneyBookUid, from, until);
    }

    @Override
    public void delete(MoneyBookTransfer transfer) {
        jpa.delete(transfer);
    }
}
