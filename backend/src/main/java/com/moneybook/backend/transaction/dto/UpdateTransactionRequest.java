package com.moneybook.backend.transaction.dto;

import com.moneybook.backend.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionRequest(
        @NotNull TransactionType transactionType,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull LocalDate transactionDate,
        Long categoryUid,
        @NotNull Long accountUid,
        @Size(max = 500) String memo,
        Long largeCategoryUid,
        Long mediumCategoryUid,
        Long smallCategoryUid
) {
    public UpdateTransactionRequest(TransactionType type, BigDecimal amount, LocalDate date, Long categoryUid,
                                    Long accountUid, String memo) {
        this(type, amount, date, categoryUid, accountUid, memo, null, null, null);
    }
}
