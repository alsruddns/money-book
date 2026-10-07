package com.moneybook.backend.calendar.repository;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.MoneyBookTransfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CalendarRepository {
    List<TransactionDay> transactionDays(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<TransferDay> transferDays(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<MoneyBookTransaction> transactionsOnDate(Long bookUid, LocalDate date);
    List<MoneyBookTransfer> transfersOnDate(Long bookUid, LocalDate date);

    record TransactionDay(LocalDate date, BigDecimal income, BigDecimal expense,
                          long count, long recurringCount) { }
    record TransferDay(LocalDate date, BigDecimal amount, long count) { }
}
