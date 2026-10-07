package com.moneybook.backend.recurring.dto;

import com.moneybook.backend.entity.RecurringTransaction;
import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(
        Long recurringTransactionUid,
        Long moneyBookUid,
        TransactionType transactionType,
        BigDecimal amount,
        Long categoryUid,
        String categoryName,
        Long accountUid,
        String accountName,
        RecurringFrequency frequency,
        Integer dayOfMonth,
        Integer dayOfWeek,
        LocalDate startDate,
        LocalDate endDate,
        String memo,
        boolean isActive,
        LocalDate lastGeneratedDate
) {
    public static RecurringTransactionResponse from(RecurringTransaction rule) {
        return new RecurringTransactionResponse(rule.getRecurringTransactionUid(),
                rule.getMoneyBook().getMoneyBookUid(), rule.getTransactionType(), rule.getAmount(),
                rule.getCategory().getCategoryUid(), rule.getCategory().getName(),
                rule.getAccount().getAccountUid(), rule.getAccount().getName(),
                rule.getFrequency(), rule.getDayOfMonth(), rule.getDayOfWeek(),
                rule.getStartDate(), rule.getEndDate(), rule.getMemo(), rule.isActive(), rule.getLastGeneratedDate());
    }
}
