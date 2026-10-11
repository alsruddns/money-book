package com.moneybook.backend.category.dto;

import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.enums.CategoryLevel;

public record CategoryResponse(Long categoryUid, Long moneyBookUid, String name,
                               TransactionType transactionType, int sortOrder, CategoryLevel level,
                               Long parentCategoryUid, boolean active) {
    public CategoryResponse(Long uid, Long bookUid, String name, TransactionType type, int sortOrder) {
        this(uid, bookUid, name, type, sortOrder, CategoryLevel.LARGE, null, true);
    }
    public static CategoryResponse from(MoneyBookCategory category) {
        return new CategoryResponse(category.getCategoryUid(), category.getMoneyBook().getMoneyBookUid(),
                category.getName(), category.getTransactionType(), category.getSortOrder(), category.getCategoryLevel(),
                category.getParentCategory() == null ? null : category.getParentCategory().getCategoryUid(), category.isActive());
    }
}
