package com.moneybook.backend.transaction.controller;

import com.moneybook.backend.account.controller.AccountController;
import com.moneybook.backend.account.dto.AccountResponse;
import com.moneybook.backend.account.service.AccountService;
import com.moneybook.backend.category.controller.CategoryController;
import com.moneybook.backend.category.dto.CategoryResponse;
import com.moneybook.backend.category.service.CategoryService;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.AccountType;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.enums.TransactionType;
import com.moneybook.backend.transaction.dto.TransactionResponse;
import com.moneybook.backend.transaction.service.TransactionService;
import com.moneybook.backend.transaction.service.TransactionSearchService;
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

@WebMvcTest({CategoryController.class, AccountController.class, TransactionController.class})
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class LedgerControllerTests {
    @Autowired private MockMvc mvc;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private CategoryService categories;
    @MockitoBean private AccountService accounts;
    @MockitoBean private TransactionService transactions;
    @MockitoBean private TransactionSearchService search;

    @Test
    void categoryEndpointsReturnDtosAndNoContentOnDelete() throws Exception {
        var response = new CategoryResponse(11L, 7L, "food", TransactionType.EXPENSE, 1);
        when(categories.create(any(), any(), any())).thenReturn(response);
        when(categories.list(any(), any(), any())).thenReturn(List.of(response));
        when(categories.update(any(), any(), any(), any())).thenReturn(response);
        String bearer = bearer(JwtTokenType.ACCESS);
        mvc.perform(post("/api/money-books/7/categories").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"food\",\"transactionType\":\"EXPENSE\",\"sortOrder\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.categoryUid").value(11));
        mvc.perform(get("/api/money-books/7/categories?transactionType=EXPENSE").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("food"));
        mvc.perform(patch("/api/money-books/7/categories/11").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"food\",\"sortOrder\":1}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/money-books/7/categories/11").contextPath("/api")
                        .header("Authorization", bearer)).andExpect(status().isNoContent());
    }

    @Test
    void accountEndpointsReturnDtosAndNoContentOnDelete() throws Exception {
        var response = new AccountResponse(12L, 7L, "cash", AccountType.CASH, 0);
        when(accounts.create(any(), any(), any())).thenReturn(response);
        when(accounts.list(any(), any())).thenReturn(List.of(response));
        when(accounts.update(any(), any(), any(), any())).thenReturn(response);
        String bearer = bearer(JwtTokenType.ACCESS);
        mvc.perform(post("/api/money-books/7/accounts").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"cash\",\"accountType\":\"CASH\",\"sortOrder\":0}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.accountUid").value(12));
        mvc.perform(get("/api/money-books/7/accounts").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("cash"));
        mvc.perform(patch("/api/money-books/7/accounts/12").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"cash\",\"accountType\":\"CASH\",\"sortOrder\":0}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/money-books/7/accounts/12").contextPath("/api")
                        .header("Authorization", bearer)).andExpect(status().isNoContent());
    }

    @Test
    void transactionEndpointsReturnDetailsMonthlyRowsAndEmptyArray() throws Exception {
        var response = new TransactionResponse(13L, 7L, TransactionType.EXPENSE, new BigDecimal("15000"),
                LocalDate.parse("2026-10-02"), 11L, "food", 12L, "cash", "dinner");
        when(transactions.create(any(), any(), any())).thenReturn(response);
        when(transactions.detail(any(), any(), any())).thenReturn(response);
        when(transactions.update(any(), any(), any(), any())).thenReturn(response);
        when(transactions.list(any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(response))
                .thenReturn(List.of());
        String bearer = bearer(JwtTokenType.ACCESS);
        String body = """
                {"transactionType":"EXPENSE","amount":15000,"transactionDate":"2026-10-02",
                 "categoryUid":11,"accountUid":12,"memo":"dinner"}
                """;
        mvc.perform(post("/api/money-books/7/transactions").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.categoryName").value("food"));
        mvc.perform(get("/api/money-books/7/transactions/13").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountName").value("cash"));
        mvc.perform(patch("/api/money-books/7/transactions/13").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(get("/api/money-books/7/transactions?year=2026&month=10").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].transactionUid").value(13));
        mvc.perform(get("/api/money-books/7/transactions?year=2026&month=11").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/money-books/7/transactions/13").contextPath("/api")
                        .header("Authorization", bearer)).andExpect(status().isNoContent());
    }

    @Test
    void requestValidationRejectsInvalidNamesAmountsDatesAndMonths() throws Exception {
        String bearer = bearer(JwtTokenType.ACCESS);
        mvc.perform(post("/api/money-books/7/categories").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"transactionType\":\"EXPENSE\",\"sortOrder\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/money-books/7/accounts").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"accountType\":\"BANK\",\"sortOrder\":-1}"))
                .andExpect(status().isBadRequest());
        for (String amount : List.of("0", "-1", "1.234")) {
            mvc.perform(post("/api/money-books/7/transactions").contextPath("/api")
                            .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"transactionType\":\"EXPENSE\",\"amount\":" + amount
                                    + ",\"transactionDate\":\"2026-10-02\",\"categoryUid\":11,\"accountUid\":12}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/money-books/7/transactions").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionType\":\"EXPENSE\",\"amount\":10,\"categoryUid\":11,\"accountUid\":12}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/money-books/7/transactions?year=2026&month=13").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/money-books/7/transactions?year=2026").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/money-books/7/categories?transactionType=TRANSFER").contextPath("/api")
                        .header("Authorization", bearer))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/money-books/7/transactions").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionType":"EXPENSE","amount":10,"transactionDate":"bad-date",
                                 "categoryUid":11,"accountUid":12}
                                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void representativeEndpointsRejectMissingAndRefreshTokens() throws Exception {
        for (String bearer : List.of("", bearer(JwtTokenType.REFRESH))) {
            mvc.perform(get("/api/money-books/7/categories").contextPath("/api")
                            .header("Authorization", bearer)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/money-books/7/accounts").contextPath("/api")
                            .header("Authorization", bearer)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/money-books/7/transactions?year=2026&month=10").contextPath("/api")
                            .header("Authorization", bearer)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/money-books/7/transactions").contextPath("/api")
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
