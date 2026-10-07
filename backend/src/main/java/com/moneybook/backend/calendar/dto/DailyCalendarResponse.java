package com.moneybook.backend.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyCalendarResponse(LocalDate date, boolean holiday, String holidayName,
                                    List<CalendarTransactionResponse> transactions,
                                    List<CalendarTransferResponse> transfers) { }
