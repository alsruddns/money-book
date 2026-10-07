package com.moneybook.backend.dashboard;

import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.dashboard.controller.DashboardController;
import com.moneybook.backend.dashboard.dto.MonthlyDashboardResponse;
import com.moneybook.backend.dashboard.service.DashboardService;
import com.moneybook.backend.enums.JwtTokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class DashboardControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private DashboardService service;

    @Test
    void accessTokenCanUseDashboardRoutes() throws Exception {
        when(service.monthly(any(), anyInt(), anyInt(), any())).thenReturn(
                new MonthlyDashboardResponse(2026, 10, BigDecimal.TEN, BigDecimal.ONE,
                        new BigDecimal("9"), 2, 1, 1));
        when(service.categories(any(), anyInt(), anyInt(), any(), any())).thenReturn(List.of());
        when(service.accounts(any(), anyInt(), anyInt(), any())).thenReturn(List.of());
        String access = bearer(JwtTokenType.ACCESS);
        mvc.perform(get("/api/money-books/7/dashboard/monthly?year=2026&month=10")
                        .contextPath("/api").header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(9));
        mvc.perform(get("/api/money-books/7/dashboard/categories?year=2026&month=10&transactionType=EXPENSE")
                        .contextPath("/api").header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/money-books/7/dashboard/accounts?year=2026&month=10")
                        .contextPath("/api").header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void invalidMonthAndMissingTypeReturnValidationError() throws Exception {
        String access = bearer(JwtTokenType.ACCESS);
        mvc.perform(get("/api/money-books/7/dashboard/monthly?year=2026&month=13")
                        .contextPath("/api").header("Authorization", access))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/money-books/7/dashboard/categories?year=2026&month=10")
                        .contextPath("/api").header("Authorization", access))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingAndRefreshBearerAreUnauthorized() throws Exception {
        for (String authorization : new String[]{"", bearer(JwtTokenType.REFRESH)}) {
            mvc.perform(get("/api/money-books/7/dashboard/monthly?year=2026&month=10")
                            .contextPath("/api").header("Authorization", authorization))
                    .andExpect(status().isUnauthorized());
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
