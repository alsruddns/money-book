package com.moneybook.backend.recurring.dto;

import com.moneybook.backend.enums.RecurringFrequency;
import com.moneybook.backend.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRecurringTransactionRequest(
        @NotNull TransactionType transactionType,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull Long categoryUid,
        @NotNull Long accountUid,
        @NotNull RecurringFrequency frequency,
        Integer dayOfMonth,
        Integer dayOfWeek,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Size(max = 500) String memo
) { }
