package com.moneybook.backend.category.repository.impl;

import com.moneybook.backend.category.repository.CategoryRepository;
import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {
    private final CategoryJpaRepository jpa;

    @Override
    public MoneyBookCategory save(MoneyBookCategory category) {
        return jpa.saveAndFlush(category);
    }

    @Override
    public void flush() {
        jpa.flush();
    }

    @Override
    public Optional<MoneyBookCategory> findById(Long categoryUid) {
        return jpa.findById(categoryUid);
    }

    @Override
    public Optional<MoneyBookCategory> findByIdAndMoneyBookUid(Long categoryUid, Long moneyBookUid) {
        return jpa.findByCategoryUidAndMoneyBook_MoneyBookUid(categoryUid, moneyBookUid);
    }

    @Override
    public List<MoneyBookCategory> findByMoneyBookUid(Long moneyBookUid, TransactionType type) {
        if (type == null) {
            return jpa.findByMoneyBook_MoneyBookUidOrderByTransactionTypeAscSortOrderAscCategoryUidAsc(moneyBookUid);
        }
        return jpa.findByMoneyBook_MoneyBookUidAndTransactionTypeOrderByTransactionTypeAscSortOrderAscCategoryUidAsc(
                moneyBookUid, type);
    }

    @Override
    public boolean existsByName(Long moneyBookUid, TransactionType type, String name, Long exceptUid) {
        return jpa.existsDuplicate(moneyBookUid, type, name, exceptUid);
    }

    @Override
    public boolean isInUse(Long categoryUid) {
        return jpa.isInUse(categoryUid);
    }

    @Override
    public void delete(MoneyBookCategory category) {
        jpa.delete(category);
    }
}
