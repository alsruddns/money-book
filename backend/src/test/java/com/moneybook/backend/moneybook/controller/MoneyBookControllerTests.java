package com.moneybook.backend.moneybook.controller;

import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.service.MoneyBookService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MoneyBookController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class MoneyBookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private MoneyBookService moneyBookService;

    @Test
    void createWithAccessTokenReturnsCreatedBook() throws Exception {
        when(moneyBookService.create(any(), any()))
                .thenReturn(new CreateMoneyBookResponse(7L, "우리집 가계부", 42L));

        mockMvc.perform(post("/api/money-books").contextPath("/api")
                        .header("Authorization", "Bearer " + token(JwtTokenType.ACCESS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"우리집 가계부\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moneyBookUid").value(7))
                .andExpect(jsonPath("$.name").value("우리집 가계부"))
                .andExpect(jsonPath("$.ownerUserUid").value(42));
    }

    @Test
    void listWithAccessTokenReturnsMembershipPermissions() throws Exception {
        when(moneyBookService.list(any())).thenReturn(List.of(
                new MoneyBookListResponse(7L, "우리집 가계부", 42L,
                        true, true, true, true, true, true)));

        mockMvc.perform(get("/api/money-books").contextPath("/api")
                        .header("Authorization", "Bearer " + token(JwtTokenType.ACCESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moneyBookUid").value(7))
                .andExpect(jsonPath("$[0].name").value("우리집 가계부"))
                .andExpect(jsonPath("$[0].ownerUserUid").value(42))
                .andExpect(jsonPath("$[0].isOwner").value(true))
                .andExpect(jsonPath("$[0].isAdmin").value(true))
                .andExpect(jsonPath("$[0].canCreate").value(true))
                .andExpect(jsonPath("$[0].canRead").value(true))
                .andExpect(jsonPath("$[0].canUpdate").value(true))
                .andExpect(jsonPath("$[0].canDelete").value(true));
    }

    @Test
    void listWithoutBooksReturnsEmptyArray() throws Exception {
        when(moneyBookService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/money-books").contextPath("/api")
                        .header("Authorization", "Bearer " + token(JwtTokenType.ACCESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createRejectsBlankAndTooLongNames() throws Exception {
        for (String name : List.of(" ", "a".repeat(101))) {
            mockMvc.perform(post("/api/money-books").contextPath("/api")
                            .header("Authorization", "Bearer " + token(JwtTokenType.ACCESS))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void endpointsRejectMissingAndRefreshBearerTokens() throws Exception {
        for (String bearer : List.of("", "Bearer " + token(JwtTokenType.REFRESH))) {
            mockMvc.perform(get("/api/money-books").contextPath("/api")
                            .header("Authorization", bearer))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/money-books").contextPath("/api")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"우리집 가계부\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String token(JwtTokenType type) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, type.name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
