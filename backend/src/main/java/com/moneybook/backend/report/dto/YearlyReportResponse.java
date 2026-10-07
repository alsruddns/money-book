package com.moneybook.backend.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record YearlyReportResponse(int year, BigDecimal totalIncome, BigDecimal totalExpense,
                                   BigDecimal balance, List<YearlyMonthResponse> months) { }
