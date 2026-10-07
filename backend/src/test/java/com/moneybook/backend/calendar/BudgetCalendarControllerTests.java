package com.moneybook.backend.calendar;

import com.moneybook.backend.budget.controller.BudgetController;
import com.moneybook.backend.budget.dto.MonthlyBudgetResponse;
import com.moneybook.backend.budget.service.BudgetService;
import com.moneybook.backend.calendar.controller.CalendarController;
import com.moneybook.backend.calendar.dto.CalendarDayResponse;
import com.moneybook.backend.calendar.dto.DailyCalendarResponse;
import com.moneybook.backend.calendar.dto.MonthlyCalendarResponse;
import com.moneybook.backend.calendar.provider.CalendarProvider;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BudgetController.class, CalendarController.class})
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class BudgetCalendarControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private BudgetService budgets;
    @MockitoBean private CalendarProvider calendar;

    @Test
    void budgetRoutesUseContextPathAndAccessAuthentication() throws Exception {
        var response = new MonthlyBudgetResponse(true, 9L, 7L, 2026, 10, new BigDecimal("100"),
                BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO, false, List.of());
        when(budgets.get(any(), anyInt(), anyInt(), any())).thenReturn(response);
        when(budgets.save(any(), anyInt(), anyInt(), any(), any())).thenReturn(response);
        String access = bearer(JwtTokenType.ACCESS);
        mvc.perform(get("/api/money-books/7/budgets/2026/10").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(true));
        mvc.perform(put("/api/money-books/7/budgets/2026/10").contextPath("/api")
                        .header("Authorization", access).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalBudget":100,"categories":[]}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.budgetUid").value(9));
        mvc.perform(put("/api/money-books/7/budgets/2026/10").contextPath("/api")
                        .header("Authorization", access).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalBudget":-1,"categories":[]}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void calendarRoutesReturnMonthlyAndDailyDtos() throws Exception {
        LocalDate date = LocalDate.parse("2026-10-01");
        var day = new CalendarDayResponse(date, DayOfWeek.THURSDAY, false, false, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, false);
        when(calendar.monthly(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MonthlyCalendarResponse(2026, 10, List.of(day)));
        when(calendar.daily(any(), any(), any())).thenReturn(
                new DailyCalendarResponse(date, false, null, List.of(), List.of()));
        String access = bearer(JwtTokenType.ACCESS);
        mvc.perform(get("/api/money-books/7/calendar?year=2026&month=10").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.days[0].date").value("2026-10-01"));
        mvc.perform(get("/api/money-books/7/calendar/2026-10-01").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.transactions").isEmpty());
        mvc.perform(get("/api/money-books/7/calendar/invalid").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingAndRefreshBearerCannotUseBudgetOrCalendarRoutes() throws Exception {
        for (String authorization : List.of("", bearer(JwtTokenType.REFRESH))) {
            mvc.perform(get("/api/money-books/7/budgets/2026/10").contextPath("/api")
                            .header("Authorization", authorization)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/money-books/7/calendar?year=2026&month=10").contextPath("/api")
                            .header("Authorization", authorization)).andExpect(status().isUnauthorized());
        }
    }

    private String bearer(JwtTokenType type) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("42").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, type.name()).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
