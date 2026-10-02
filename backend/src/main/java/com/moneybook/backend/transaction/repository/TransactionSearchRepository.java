package com.moneybook.backend.transaction.repository;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import java.util.List;
import java.time.LocalDate;

public interface TransactionSearchRepository {
    SearchResult search(Long bookUid, TransactionSearchRequest request);
    List<MoneyBookTransaction> findNextBatch(Long bookUid, TransactionSearchRequest filters,
                                             LocalDate afterDate, Long afterUid, int limit);

    record SearchResult(List<MoneyBookTransaction> rows, long total) { }
}
