package com.moneybook.backend.calendar.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record CalendarDayResponse(LocalDate date, DayOfWeek dayOfWeek, boolean weekend,
                                  boolean holiday, String holidayName,
                                  BigDecimal incomeAmount, BigDecimal expenseAmount,
                                  BigDecimal transferInAmount, BigDecimal transferOutAmount,
                                  long transactionCount, long transferCount,
                                  boolean hasRecurringGeneratedTransaction) { }
