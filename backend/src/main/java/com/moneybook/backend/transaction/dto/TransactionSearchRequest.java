package com.moneybook.backend.transaction.dto;

import com.moneybook.backend.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Inclusive date range and bounded, allowlisted search options. */
public record TransactionSearchRequest(LocalDate startDate, LocalDate endDate,
                                       TransactionType transactionType, Long categoryUid, Long accountUid,
                                       String keyword, BigDecimal minAmount, BigDecimal maxAmount,
                                       int page, int size, TransactionSearchSort sort) { }
