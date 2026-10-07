package com.moneybook.backend.category.dto;

import com.moneybook.backend.entity.MoneyBookCategory;
import com.moneybook.backend.enums.TransactionType;

public record CategoryResponse(Long categoryUid, Long moneyBookUid, String name,
                               TransactionType transactionType, int sortOrder) {
    public static CategoryResponse from(MoneyBookCategory category) {
        return new CategoryResponse(category.getCategoryUid(), category.getMoneyBook().getMoneyBookUid(),
                category.getName(), category.getTransactionType(), category.getSortOrder());
    }
}
