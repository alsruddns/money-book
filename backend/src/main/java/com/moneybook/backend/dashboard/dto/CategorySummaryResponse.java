package com.moneybook.backend.dashboard.dto;

import com.moneybook.backend.enums.TransactionType;
import java.math.BigDecimal;

/** Ratio is a fraction of the selected type's monthly total, rounded to four decimal places. */
public record CategorySummaryResponse(Long categoryUid, String categoryName, TransactionType transactionType,
                                      BigDecimal totalAmount, long transactionCount, BigDecimal ratio) { }
