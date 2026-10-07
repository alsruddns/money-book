package com.moneybook.backend.report.dto;

import java.math.BigDecimal;

public record AccountStatisticsResponse(Long accountUid, String accountName, BigDecimal incomeAmount,
                                        BigDecimal expenseAmount, BigDecimal transferInAmount,
                                        BigDecimal transferOutAmount, BigDecimal netChange) { }
