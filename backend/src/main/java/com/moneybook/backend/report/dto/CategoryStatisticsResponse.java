package com.moneybook.backend.report.dto;

import com.moneybook.backend.enums.TransactionType;
import java.math.BigDecimal;

/** Ratio is a 0..1 share of the selected transaction type's period total. */
public record CategoryStatisticsResponse(Long categoryUid, String categoryName, TransactionType transactionType,
                                         BigDecimal totalAmount, long transactionCount, BigDecimal ratio) { }
