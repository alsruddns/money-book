package com.moneybook.backend.calendar.dto;

import com.moneybook.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalendarTransactionResponse(Long transactionUid, TransactionType transactionType,
                                          BigDecimal amount, Long categoryUid, String categoryName,
                                          Long accountUid, String accountName, String memo,
                                          Long recurringTransactionUid, LocalDate scheduledDate) { }
