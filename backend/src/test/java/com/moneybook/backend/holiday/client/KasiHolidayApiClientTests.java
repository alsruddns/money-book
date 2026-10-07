package com.moneybook.backend.holiday.client;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KasiHolidayApiClientTests {
    private final KasiHolidayApiClient client = new KasiHolidayApiClient("https://example.invalid/holidays", "test-key");

    @Test
    void parsesKasiXmlAndKeepsOnlyPublicHolidays() {
        var page = client.parse("""
                <response><header><resultCode>00</resultCode></header><body>
                  <totalCount>2</totalCount><items>
                    <item><locdate>20261003</locdate><dateName>개천절</dateName>
                      <dateKind>01</dateKind><isHoliday>Y</isHoliday></item>
                    <item><locdate>20261004</locdate><dateName>기념일</dateName>
                      <dateKind>02</dateKind><isHoliday>N</isHoliday></item>
                  </items></body></response>
                """, 2026);
        assertEquals(2, page.totalCount());
        assertEquals(1, page.holidays().size());
        assertEquals(LocalDate.parse("2026-10-03"), page.holidays().getFirst().date());
        assertEquals("개천절", page.holidays().getFirst().name());
    }

    @Test
    void rejectsInvalidXmlApiErrorsAndOutOfYearDates() {
        assertThrows(HolidayApiException.class, () -> client.parse("<broken", 2026));
        assertThrows(HolidayApiException.class, () -> client.parse("""
                <response><header><resultCode>20</resultCode></header><body><totalCount>0</totalCount></body></response>
                """, 2026));
        assertThrows(HolidayApiException.class, () -> client.parse("""
                <response><header><resultCode>00</resultCode></header><body><totalCount>1</totalCount>
                <items><item><locdate>20271003</locdate><dateName>개천절</dateName>
                <dateKind>01</dateKind><isHoliday>Y</isHoliday></item></items></body></response>
                """, 2026));
        assertThrows(HolidayApiException.class, () -> client.parse("""
                <!DOCTYPE response [<!ENTITY attack SYSTEM "file:///etc/passwd">]>
                <response><header><resultCode>00</resultCode></header><body><totalCount>0</totalCount></body></response>
                """, 2026));
    }
}
