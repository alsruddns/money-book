package com.moneybook.backend.holiday.client;

import java.time.LocalDate;
import java.util.List;

public interface HolidayApiClient {
    boolean configured();
    List<HolidayData> fetchYear(int year);

    record HolidayData(LocalDate date, String name, String type) { }
}
