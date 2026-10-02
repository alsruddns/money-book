package com.moneybook.backend.holiday;

import com.moneybook.backend.entity.Holiday;
import com.moneybook.backend.entity.HolidaySyncStatus;
import com.moneybook.backend.holiday.client.HolidayApiClient;
import com.moneybook.backend.holiday.client.HolidayApiException;
import com.moneybook.backend.holiday.repository.HolidayRepository;
import com.moneybook.backend.holiday.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:holiday_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class HolidayIntegrationTests {
    @Autowired private HolidayService service;
    @Autowired private HolidayRepository holidays;
    @MockitoBean private HolidayApiClient api;

    @Test
    void missingApiKeyUsesOnlyCachedRows() {
        holidays.replaceYear(2026, List.of(Holiday.fromKasi(LocalDate.parse("2026-10-03"), "개천절", "01")));
        var rows = service.forMonth(YearMonth.of(2026, 10));
        assertEquals(1, rows.size());
        assertEquals("개천절", rows.getFirst().name());
        assertTrue(service.forMonth(YearMonth.of(2025, 10)).isEmpty());
        verify(api, never()).fetchYear(2026);
    }

    @Test
    void successfulSyncDeduplicatesRowsAndCachesEmptyYears() {
        when(api.configured()).thenReturn(true);
        var item = new HolidayApiClient.HolidayData(LocalDate.parse("2026-10-03"), "개천절", "01");
        when(api.fetchYear(2026)).thenReturn(List.of(item, item));
        assertEquals(1, service.forMonth(YearMonth.of(2026, 10)).size());
        assertEquals(1, service.forMonth(YearMonth.of(2026, 10)).size());
        verify(api).fetchYear(2026);
        assertEquals(1, holidays.findBetween(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-01")).size());
        when(api.fetchYear(2027)).thenReturn(List.of());
        assertTrue(service.forMonth(YearMonth.of(2027, 10)).isEmpty());
        assertTrue(service.forMonth(YearMonth.of(2027, 10)).isEmpty());
        verify(api).fetchYear(2027);
    }

    @Test
    void apiFailureFallsBackToCacheAndThrottlesRetry() {
        when(api.configured()).thenReturn(true);
        LocalDateTime old = now().minusDays(8);
        HolidaySyncStatus status = HolidaySyncStatus.attempted(2026, old);
        status.markSynced(old);
        holidays.saveStatus(status);
        holidays.replaceYear(2026, List.of(Holiday.fromKasi(LocalDate.parse("2026-10-03"), "기존 공휴일", "01")));
        when(api.fetchYear(2026)).thenThrow(new HolidayApiException("timeout"));
        assertEquals("기존 공휴일", service.forMonth(YearMonth.of(2026, 10)).getFirst().name());
        assertEquals("기존 공휴일", service.forMonth(YearMonth.of(2026, 10)).getFirst().name());
        verify(api).fetchYear(2026);
    }

    @Test
    void apiFailureWithoutCacheStillReturnsEmptyCalendarHolidays() {
        when(api.configured()).thenReturn(true);
        when(api.fetchYear(2026)).thenThrow(new HolidayApiException("invalid response"));
        assertTrue(service.forMonth(YearMonth.of(2026, 10)).isEmpty());
    }

    @Test
    void pastYearCacheRemainsWhileCurrentYearRefreshesAfterSevenDays() {
        when(api.configured()).thenReturn(true);
        int current = now().getYear();
        int past = current - 1;
        LocalDateTime old = now().minusDays(8);
        HolidaySyncStatus pastStatus = HolidaySyncStatus.attempted(past, old);
        pastStatus.markSynced(old);
        holidays.saveStatus(pastStatus);
        HolidaySyncStatus currentStatus = HolidaySyncStatus.attempted(current, old);
        currentStatus.markSynced(old);
        holidays.saveStatus(currentStatus);
        when(api.fetchYear(current)).thenReturn(List.of(new HolidayApiClient.HolidayData(
                LocalDate.of(current, 10, 3), "새 공휴일", "01")));
        assertTrue(service.forMonth(YearMonth.of(past, 10)).isEmpty());
        assertEquals("새 공휴일", service.forMonth(YearMonth.of(current, 10)).getFirst().name());
        verify(api, never()).fetchYear(past);
        verify(api).fetchYear(current);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
