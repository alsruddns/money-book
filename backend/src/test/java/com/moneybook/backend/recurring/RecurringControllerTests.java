package com.moneybook.backend.recurring;

import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.recurring.controller.RecurringTransactionController;
import com.moneybook.backend.recurring.dto.GenerateRecurringTransactionResponse;
import com.moneybook.backend.recurring.service.RecurringTransactionService;
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

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecurringTransactionController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class RecurringControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private RecurringTransactionService service;

    @Test
    void generateRouteAcceptsAccessTokenAndReturnsCount() throws Exception {
        when(service.generate(any(), any(), any())).thenReturn(
                new GenerateRecurringTransactionResponse(LocalDate.parse("2026-10-02"), 2));
        mvc.perform(post("/api/money-books/7/recurring-transactions/generate").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"baseDate\":\"2026-10-02\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generatedCount").value(2));
    }

    @Test
    void invalidRecurringRequestReturnsValidationError() throws Exception {
        mvc.perform(post("/api/money-books/7/recurring-transactions").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingAndRefreshBearerCannotAccessRecurringRoutes() throws Exception {
        for (String authorization : new String[]{"", bearer(JwtTokenType.REFRESH)}) {
            mvc.perform(get("/api/money-books/7/recurring-transactions").contextPath("/api")
                    .header("Authorization", authorization)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/money-books/7/recurring-transactions/generate").contextPath("/api")
                            .header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"baseDate\":\"2026-10-02\"}"))
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
