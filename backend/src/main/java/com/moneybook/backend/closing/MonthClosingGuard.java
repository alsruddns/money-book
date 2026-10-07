package com.moneybook.backend.closing;

import com.moneybook.backend.closing.repository.MonthClosingRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Checks the immutable-month policy inside the caller's write transaction. */
@Component
@RequiredArgsConstructor
public class MonthClosingGuard {
    private final MonthClosingRepository closings;

    public void requireOpen(Long bookUid, LocalDate date) {
        requireOpen(bookUid, Set.of(YearMonth.from(date)));
    }

    public void requireOpen(Long bookUid, Collection<YearMonth> periods) {
        if (periods.isEmpty()) return;
        closings.lockBook(bookUid);
        Set<YearMonth> requested = new HashSet<>(periods);
        int firstYear = requested.stream().mapToInt(YearMonth::getYear).min().orElseThrow();
        int lastYear = requested.stream().mapToInt(YearMonth::getYear).max().orElseThrow();
        for (YearMonth closed : closings.findClosedYears(bookUid, firstYear, lastYear)) {
            if (requested.contains(closed)) throw new BusinessException(ErrorCode.MONTH_CLOSED);
        }
    }
}
