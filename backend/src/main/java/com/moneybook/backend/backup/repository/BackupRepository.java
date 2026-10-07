package com.moneybook.backend.backup.repository;

import com.moneybook.backend.entity.MoneyBook;
import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.entity.MoneyBookTransfer;
import java.util.List;
import java.util.Optional;

/** Persistence boundary for reading and atomically restoring a complete MoneyBook dataset. */
public interface BackupRepository {
    Optional<MoneyBook> findBook(Long moneyBookUid);
    <T> List<T> findRows(Class<T> entityType, Long moneyBookUid);
    List<MoneyBookTransaction> transactionBatch(Long moneyBookUid, Long afterUid, int limit);
    List<MoneyBookTransfer> transferBatch(Long moneyBookUid, Long afterUid, int limit);
    <T> T save(T entity);
    void flush();
    void clear();
}
