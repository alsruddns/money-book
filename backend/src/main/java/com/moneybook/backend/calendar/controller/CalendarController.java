package com.moneybook.backend.calendar.controller;

import com.moneybook.backend.calendar.dto.DailyCalendarResponse;
import com.moneybook.backend.calendar.dto.MonthlyCalendarResponse;
import com.moneybook.backend.calendar.provider.CalendarProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 가계부 거래, 이체, 생성된 정기 거래 및 한국 공휴일의 캘린더 API를 제공한다. */
@RestController
@RequestMapping("/money-books/{moneyBookUid}/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarProvider calendarProvider;

    /** R 권한으로 해당 월 모든 날짜의 거래·이체 합계와 공휴일을 조회한다. */
    @GetMapping
    public MonthlyCalendarResponse monthly(@PathVariable Long moneyBookUid, @RequestParam int year,
                                           @RequestParam int month, Authentication authentication) {
        return calendarProvider.monthly(moneyBookUid, year, month, authentication);
    }

    /** R 권한으로 해당 날짜의 거래·이체 상세와 공휴일을 조회한다. */
    @GetMapping("/{date}")
    public DailyCalendarResponse daily(@PathVariable Long moneyBookUid,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       Authentication authentication) {
        return calendarProvider.daily(moneyBookUid, date, authentication);
    }
}
