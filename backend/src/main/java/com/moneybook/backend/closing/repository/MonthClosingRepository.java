package com.moneybook.backend.closing.repository;

import com.moneybook.backend.entity.MoneyBookMonthClosing;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface MonthClosingRepository {
    void lockBook(Long bookUid);
    boolean exists(Long bookUid, int year, int month);
    List<YearMonth> findClosedYears(Long bookUid, int firstYear, int lastYear);
    Optional<MoneyBookMonthClosing> find(Long bookUid, int year, int month);
    MoneyBookMonthClosing save(MoneyBookMonthClosing closing);
    void delete(MoneyBookMonthClosing closing);
}
