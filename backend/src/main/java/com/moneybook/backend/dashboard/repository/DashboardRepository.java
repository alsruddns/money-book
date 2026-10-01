package com.moneybook.backend.dashboard.repository;

import com.moneybook.backend.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DashboardRepository {
    List<TypeTotal> transactionTotals(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<CategoryTotal> categoryTotals(Long bookUid, LocalDate start, LocalDate endExclusive, TransactionType type);
    List<AccountTransactionTotal> accountTransactionTotals(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<AccountTransferTotal> transferInTotals(Long bookUid, LocalDate start, LocalDate endExclusive);
    List<AccountTransferTotal> transferOutTotals(Long bookUid, LocalDate start, LocalDate endExclusive);

    record TypeTotal(TransactionType type, BigDecimal amount, long count) { }
    record CategoryTotal(Long uid, String name, TransactionType type, BigDecimal amount, long count) { }
    record AccountTransactionTotal(Long uid, TransactionType type, BigDecimal amount) { }
    record AccountTransferTotal(Long uid, BigDecimal amount) { }
}
