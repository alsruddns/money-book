package com.moneybook.backend.holiday.service.impl;

import com.moneybook.backend.entity.Holiday;
import com.moneybook.backend.entity.HolidaySyncStatus;
import com.moneybook.backend.holiday.client.HolidayApiClient;
import com.moneybook.backend.holiday.client.HolidayApiException;
import com.moneybook.backend.holiday.repository.HolidayRepository;
import com.moneybook.backend.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final Duration RETRY_DELAY = Duration.ofHours(1);
    private static final Duration REFRESH_INTERVAL = Duration.ofDays(7);

    private final HolidayRepository holidays;
    private final HolidayApiClient client;

    /** Lazy yearly synchronization uses a one-hour retry cooldown and returns cached rows on API failure. */
    @Override
    @Transactional
    public List<HolidayEntry> forMonth(YearMonth month) {
        int year = month.getYear();
        LocalDateTime now = LocalDateTime.now(KOREA);
        HolidaySyncStatus status = holidays.findStatus(year).orElse(null);
        if (client.configured() && shouldSync(year, status, now)) {
            if (status == null) {
                status = HolidaySyncStatus.attempted(year, now);
                holidays.saveStatus(status);
            } else {
                status.markAttempted(now);
            }
            try {
                List<HolidayApiClient.HolidayData> fetched = client.fetchYear(year);
                Set<String> seen = new HashSet<>();
                List<Holiday> fresh = new ArrayList<>();
                for (HolidayApiClient.HolidayData item : fetched) {
                    if (seen.add(item.date() + "|" + item.name())) {
                        fresh.add(Holiday.fromKasi(item.date(), item.name(), item.type()));
                    }
                }
                holidays.replaceYear(year, fresh);
                status.markSynced(now);
            } catch (HolidayApiException exception) {
                log.warn("KASI holiday sync failed for year {}: {}", year, exception.getMessage());
            }
        }
        LocalDate start = month.atDay(1);
        return holidays.findBetween(start, month.plusMonths(1).atDay(1)).stream()
                .map(item -> new HolidayEntry(item.getHolidayDate(), item.getName())).toList();
    }

    private boolean shouldSync(int year, HolidaySyncStatus status, LocalDateTime now) {
        if (status == null) return true;
        if (status.getLastAttemptAt().plus(RETRY_DELAY).isAfter(now)) return false;
        if (status.getLastSyncedAt() == null) return true;
        int currentYear = now.getYear();
        return year >= currentYear && year <= currentYear + 1
                && !status.getLastSyncedAt().plus(REFRESH_INTERVAL).isAfter(now);
    }
}
