package com.moneybook.backend.category.repository;

import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.TransactionType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CategoryRepository {
    MoneyBookCategory save(MoneyBookCategory category);
    void flush();
    Optional<MoneyBookCategory> findById(Long categoryUid);
    Optional<MoneyBookCategory> findByIdAndMoneyBookUid(Long categoryUid, Long moneyBookUid);
    List<MoneyBookCategory> findAllByMoneyBookUidAndIds(Long moneyBookUid, Set<Long> categoryUids);
    List<MoneyBookCategory> findByMoneyBookUid(Long moneyBookUid, TransactionType transactionType);
    boolean existsByName(Long moneyBookUid, TransactionType type, String name, Long exceptCategoryUid);
    boolean isInUse(Long categoryUid);
    void delete(MoneyBookCategory category);
}
