package com.moneybook.backend.transaction.dto;

import com.moneybook.backend.entity.MoneyBookTransaction;
import com.moneybook.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long transactionUid,
        Long moneyBookUid,
        TransactionType transactionType,
        BigDecimal amount,
        LocalDate transactionDate,
        Long categoryUid,
        String categoryName,
        Long accountUid,
        String accountName,
        String memo,
        CategoryPath categoryPath
) {
    public TransactionResponse(Long transactionUid, Long moneyBookUid, TransactionType transactionType,
                               BigDecimal amount, LocalDate transactionDate, Long categoryUid, String categoryName,
                               Long accountUid, String accountName, String memo) {
        this(transactionUid, moneyBookUid, transactionType, amount, transactionDate, categoryUid, categoryName,
                accountUid, accountName, memo, new CategoryPath(new CategoryItem(categoryUid, categoryName), null, null));
    }
    public record CategoryPath(CategoryItem largeCategory, CategoryItem mediumCategory, CategoryItem smallCategory) { }
    public record CategoryItem(Long categoryUid, String name) { }

    public static TransactionResponse from(MoneyBookTransaction entry) {
        var category = entry.getCategory();
        var parent = category.getParentCategory();
        var large = parent == null ? category : parent.getParentCategory() == null ? parent : parent.getParentCategory();
        var medium = category.getParentCategory() == null ? null : parent.getParentCategory() == null ? category : parent;
        var small = category.getCategoryLevel() == com.moneybook.backend.enums.CategoryLevel.SMALL ? category : null;
        return new TransactionResponse(entry.getTransactionUid(), entry.getMoneyBook().getMoneyBookUid(),
                entry.getTransactionType(), entry.getAmount(), entry.getTransactionDate(),
                category.getCategoryUid(), category.getName(), entry.getAccount().getAccountUid(), entry.getAccount().getName(),
                entry.getMemo(), new CategoryPath(item(large), item(medium), item(small)));
    }
    private static CategoryItem item(com.moneybook.backend.entity.MoneyBookCategory category) {
        return category == null ? null : new CategoryItem(category.getCategoryUid(), category.getName());
    }
}
