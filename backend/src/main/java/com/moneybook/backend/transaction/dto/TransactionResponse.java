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
        String memo
) {
    public static TransactionResponse from(MoneyBookTransaction entry) {
        return new TransactionResponse(entry.getTransactionUid(), entry.getMoneyBook().getMoneyBookUid(),
                entry.getTransactionType(), entry.getAmount(), entry.getTransactionDate(),
                entry.getCategory().getCategoryUid(), entry.getCategory().getName(),
                entry.getAccount().getAccountUid(), entry.getAccount().getName(), entry.getMemo());
    }
}
