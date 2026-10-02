package com.moneybook.backend.backup.repository;

import com.moneybook.backend.entity.MoneyBook;
import java.util.List;
import java.util.Optional;

/** Persistence boundary for reading and atomically restoring a complete MoneyBook dataset. */
public interface BackupRepository {
    Optional<MoneyBook> findBook(Long moneyBookUid);
    <T> List<T> findRows(Class<T> entityType, Long moneyBookUid);
    <T> T save(T entity);
    void flush();
    void clear();
}
