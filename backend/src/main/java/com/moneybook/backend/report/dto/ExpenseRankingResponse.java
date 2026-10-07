package com.moneybook.backend.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRankingResponse(int rank, Long transactionUid, LocalDate transactionDate,
                                     Long categoryUid, String categoryName, Long accountUid,
                                     String accountName, String memo, BigDecimal amount) { }
