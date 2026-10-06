package com.moneybook.backend.dashboard.dto;

import java.math.BigDecimal;

public record AccountSummaryResponse(Long accountUid, String accountName, BigDecimal incomeAmount,
                                     BigDecimal expenseAmount, BigDecimal transferInAmount,
                                     BigDecimal transferOutAmount) { }
