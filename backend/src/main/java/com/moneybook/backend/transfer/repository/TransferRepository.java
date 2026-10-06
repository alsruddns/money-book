package com.moneybook.backend.transfer.repository;

import com.moneybook.backend.entity.MoneyBookTransfer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransferRepository {
    MoneyBookTransfer save(MoneyBookTransfer transfer);
    Optional<MoneyBookTransfer> findByIdAndMoneyBookUid(Long transferUid, Long moneyBookUid);
    List<MoneyBookTransfer> findForPeriod(Long moneyBookUid, LocalDate from, LocalDate until);
    void delete(MoneyBookTransfer transfer);
}
