package com.moneybook.backend.holiday.repository;

import com.moneybook.backend.entity.Holiday;
import com.moneybook.backend.entity.HolidaySyncStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository {
    List<Holiday> findBetween(LocalDate start, LocalDate endExclusive);
    Optional<HolidaySyncStatus> findStatus(int year);
    void saveStatus(HolidaySyncStatus status);
    void replaceYear(int year, List<Holiday> holidays);
}
