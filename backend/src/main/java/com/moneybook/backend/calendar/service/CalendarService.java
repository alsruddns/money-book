package com.moneybook.backend.calendar.service;

import com.moneybook.backend.calendar.dto.DailyCalendarResponse;
import com.moneybook.backend.calendar.dto.MonthlyCalendarResponse;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface CalendarService {
    MonthlyCalendarResponse monthly(Long bookUid, int year, int month, Authentication authentication);
    DailyCalendarResponse daily(Long bookUid, LocalDate date, Authentication authentication);
}
