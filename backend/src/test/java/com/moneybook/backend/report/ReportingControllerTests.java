package com.moneybook.backend.report;

import com.moneybook.backend.closing.controller.MonthClosingController;
import com.moneybook.backend.closing.service.MonthClosingService;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.report.controller.ReportController;
import com.moneybook.backend.report.service.ReportService;
import com.moneybook.backend.transaction.controller.TransactionController;
import com.moneybook.backend.transaction.service.TransactionSearchService;
import com.moneybook.backend.transaction.service.TransactionService;
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

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TransactionController.class, ReportController.class, MonthClosingController.class})
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class ReportingControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private TransactionService transactions;
    @MockitoBean private TransactionSearchService search;
    @MockitoBean private ReportService reports;
    @MockitoBean private MonthClosingService closings;

    @Test
    void routesAcceptAccessTokenAndKeepApiContextPathOutsideController() throws Exception {
        String bearer = bearer(JwtTokenType.ACCESS);
        mvc.perform(get("/api/money-books/7/transactions/search?startDate=2026-10-01&endDate=2026-10-31")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/money-books/7/reports/yearly?year=2026")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/money-books/7/reports/categories?startDate=2026-10-01&endDate=2026-10-31&transactionType=EXPENSE")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/money-books/7/reports/accounts?startDate=2026-10-01&endDate=2026-10-31")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/money-books/7/reports/monthly?year=2026&month=10")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(post("/api/money-books/7/month-closings/2026/10")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isCreated());
        mvc.perform(get("/api/money-books/7/month-closings/2026/10")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(delete("/api/money-books/7/month-closings/2026/10")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isNoContent());
    }

    @Test
    void malformedParametersAndRefreshTokensAreRejected() throws Exception {
        String bearer = bearer(JwtTokenType.ACCESS);
        mvc.perform(get("/api/money-books/7/transactions/search?startDate=bad&endDate=2026-10-31")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/money-books/7/reports/categories?startDate=2026-10-01&endDate=2026-10-31&transactionType=OTHER")
                .contextPath("/api").header("Authorization", bearer)).andExpect(status().isBadRequest());
        for (String path : new String[] {
                "/api/money-books/7/transactions/search?startDate=2026-10-01&endDate=2026-10-31",
                "/api/money-books/7/reports/yearly?year=2026",
                "/api/money-books/7/month-closings/2026/10"}) {
            mvc.perform(get(path).contextPath("/api")
                    .header("Authorization", bearer(JwtTokenType.REFRESH))).andExpect(status().isUnauthorized());
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
