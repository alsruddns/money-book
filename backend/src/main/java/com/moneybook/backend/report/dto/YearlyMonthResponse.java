package com.moneybook.backend.report.dto;

import java.math.BigDecimal;

public record YearlyMonthResponse(int month, BigDecimal income, BigDecimal expense,
                                  BigDecimal balance, long transactionCount) { }
