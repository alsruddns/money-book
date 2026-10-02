package com.moneybook.backend.accountmanagement.controller;

import com.moneybook.backend.accountmanagement.dto.RefreshSessionResponse;
import com.moneybook.backend.accountmanagement.service.AccountSessionService;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountSessionController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AccountSessionControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @MockitoBean private AccountSessionService service;

    @Test
    void listsOnlySafeSessionMetadataAndMarksCurrentSession() throws Exception {
        when(service.list(any())).thenReturn(List.of(new RefreshSessionResponse(7L, true, "Browser", "127.0.0.1",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now().plusDays(14))));

        mockMvc.perform(get("/api/account/sessions").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionUid").value(7))
                .andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[0].refreshTokenHash").doesNotExist())
                .andExpect(jsonPath("$[0].refreshToken").doesNotExist());
    }

    @Test
    void sessionManagementRequiresAccessToken() throws Exception {
        mockMvc.perform(get("/api/account/sessions").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/account/sessions/logout-all").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.REFRESH)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionRevocationRoutesReturnNoContent() throws Exception {
        String bearer = bearer(JwtTokenType.ACCESS);
        mockMvc.perform(delete("/api/account/sessions/7").contextPath("/api").header("Authorization", bearer))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/account/sessions/logout-all").contextPath("/api").header("Authorization", bearer))
                .andExpect(status().isNoContent());
    }

    private String bearer(JwtTokenType type) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("42").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, type.name()).claim("sid", "session-key").build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
