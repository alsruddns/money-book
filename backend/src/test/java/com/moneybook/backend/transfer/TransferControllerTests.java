package com.moneybook.backend.transfer;

import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.transfer.controller.TransferController;
import com.moneybook.backend.transfer.dto.TransferResponse;
import com.moneybook.backend.transfer.service.TransferService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class TransferControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private TransferService service;

    @Test
    void allTransferRoutesUseAccessTokenAndReturnDtos() throws Exception {
        TransferResponse response = new TransferResponse(9L, 7L, 1L, "cash", 2L, "bank",
                new BigDecimal("50000"), LocalDate.parse("2026-10-02"), "move");
        when(service.create(any(), any(), any())).thenReturn(response);
        when(service.detail(any(), any(), any())).thenReturn(response);
        when(service.update(any(), any(), any(), any())).thenReturn(response);
        when(service.list(any(), anyInt(), anyInt(), any())).thenReturn(List.of(response)).thenReturn(List.of());
        String access = bearer(JwtTokenType.ACCESS);
        String body = """
                {"fromAccountUid":1,"toAccountUid":2,"amount":50000,
                 "transferDate":"2026-10-02","memo":"move"}
                """;
        mvc.perform(post("/api/money-books/7/transfers").contextPath("/api")
                        .header("Authorization", access).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.fromAccountName").value("cash"));
        mvc.perform(get("/api/money-books/7/transfers/9").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.toAccountName").value("bank"));
        mvc.perform(patch("/api/money-books/7/transfers/9").contextPath("/api")
                        .header("Authorization", access).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(get("/api/money-books/7/transfers?year=2026&month=10").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].transferUid").value(9));
        mvc.perform(get("/api/money-books/7/transfers?year=2026&month=11").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/money-books/7/transfers/9").contextPath("/api")
                        .header("Authorization", access)).andExpect(status().isNoContent());
    }

    @Test
    void transferRequestAndMonthValidationReturnCommonError() throws Exception {
        String access = bearer(JwtTokenType.ACCESS);
        for (String amount : List.of("0", "-1", "1.234")) {
            mvc.perform(post("/api/money-books/7/transfers").contextPath("/api")
                            .header("Authorization", access).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fromAccountUid\":1,\"toAccountUid\":2,\"amount\":" + amount
                                    + ",\"transferDate\":\"2026-10-02\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        mvc.perform(get("/api/money-books/7/transfers?year=2026&month=13").contextPath("/api")
                        .header("Authorization", access))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingOrRefreshBearerCannotUseTransferEndpoints() throws Exception {
        for (String bearer : List.of("", bearer(JwtTokenType.REFRESH))) {
            mvc.perform(get("/api/money-books/7/transfers?year=2026&month=10").contextPath("/api")
                            .header("Authorization", bearer)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/money-books/7/transfers").contextPath("/api")
                            .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
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
