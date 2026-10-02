package com.moneybook.backend.holiday.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface HolidayService {
    List<HolidayEntry> forMonth(YearMonth month);

    record HolidayEntry(LocalDate date, String name) { }
}
