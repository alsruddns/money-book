package com.moneybook.backend.calendar.provider;

import com.moneybook.backend.calendar.dto.CalendarDayResponse;
import com.moneybook.backend.calendar.dto.DailyCalendarResponse;
import com.moneybook.backend.calendar.dto.MonthlyCalendarResponse;
import com.moneybook.backend.calendar.service.CalendarService;
import com.moneybook.backend.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Combines authorized ledger calendar data with shared system holiday data. */
@Component
@RequiredArgsConstructor
public class CalendarProvider {
    private final CalendarService calendarService;
    private final HolidayService holidayService;

    public MonthlyCalendarResponse monthly(Long bookUid, int year, int month, Authentication authentication) {
        MonthlyCalendarResponse ledger = calendarService.monthly(bookUid, year, month, authentication);
        Map<LocalDate, String> names = holidayNames(YearMonth.of(year, month));
        List<CalendarDayResponse> days = ledger.days().stream().map(day -> new CalendarDayResponse(
                day.date(), day.dayOfWeek(), day.weekend(), names.containsKey(day.date()), names.get(day.date()),
                day.incomeAmount(), day.expenseAmount(), day.transferInAmount(), day.transferOutAmount(),
                day.transactionCount(), day.transferCount(), day.hasRecurringGeneratedTransaction())).toList();
        return new MonthlyCalendarResponse(year, month, days);
    }

    public DailyCalendarResponse daily(Long bookUid, LocalDate date, Authentication authentication) {
        DailyCalendarResponse ledger = calendarService.daily(bookUid, date, authentication);
        Map<LocalDate, String> names = holidayNames(YearMonth.from(date));
        return new DailyCalendarResponse(date, names.containsKey(date), names.get(date),
                ledger.transactions(), ledger.transfers());
    }

    private Map<LocalDate, String> holidayNames(YearMonth month) {
        return holidayService.forMonth(month).stream().collect(Collectors.groupingBy(
                HolidayService.HolidayEntry::date,
                Collectors.mapping(HolidayService.HolidayEntry::name, Collectors.joining(", "))));
    }
}
