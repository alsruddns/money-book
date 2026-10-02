package com.moneybook.backend.transaction.repository;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.transaction.dto.TransactionSearchRequest;
import java.util.List;

public interface TransactionSearchRepository {
    SearchResult search(Long bookUid, TransactionSearchRequest request);

    record SearchResult(List<MoneyBookTransaction> rows, long total) { }
}
