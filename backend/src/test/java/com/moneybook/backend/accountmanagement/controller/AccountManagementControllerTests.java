package com.moneybook.backend.accountmanagement.controller;

import com.moneybook.backend.accountmanagement.dto.AccountMeResDto;
import com.moneybook.backend.accountmanagement.service.AccountManagementService;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.AuthProvider;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.enums.SystemRole;
import com.moneybook.backend.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountManagementController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AccountManagementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AccountManagementService accountManagementService;

    @Test
    void meReturnsSafeAccountDto() throws Exception {
        when(accountManagementService.me(any())).thenReturn(accountResponse());

        mockMvc.perform(get("/api/account/me").contextPath("/api")
                        .header("Authorization", bearer(SystemRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userUid").value(42))
                .andExpect(jsonPath("$.nickname").value("회원"))
                .andExpect(jsonPath("$.providers[0]").value("LOCAL"))
                .andExpect(jsonPath("$.loginId").value("member-id"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.providerUserId").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void accountApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/account/me").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/account/profile").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"새 이름\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/account/password").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"new\",\"newPasswordConfirm\":\"new\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileUpdateIsAvailableToAllSystemRoles() throws Exception {
        when(accountManagementService.updateProfile(any(), any())).thenReturn(accountResponse());

        for (SystemRole role : SystemRole.values()) {
            mockMvc.perform(patch("/api/account/profile").contextPath("/api")
                            .header("Authorization", bearer(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"회원\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("회원"));
        }

        verify(accountManagementService, times(3)).updateProfile(any(), any());
    }

    @Test
    void profileValidationRejectsBlankAndTooLongNickname() throws Exception {
        String bearer = bearer(SystemRole.USER);
        mockMvc.perform(patch("/api/account/profile").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(patch("/api/account/profile").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "가".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void passwordValidationRejectsMissingBlankAndOversizedValues() throws Exception {
        String bearer = bearer(SystemRole.USER);
        mockMvc.perform(patch("/api/account/password").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(patch("/api/account/password").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\" \",\"newPasswordConfirm\":\"new\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/account/password").contextPath("/api")
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"" + "x".repeat(73)
                                + "\",\"newPasswordConfirm\":\"" + "x".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawalRequiresCurrentPasswordAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/account").contextPath("/api")
                        .header("Authorization", bearer(SystemRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(delete("/api/account").contextPath("/api")
                        .header("Authorization", bearer(SystemRole.USER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"current\"}"))
                .andExpect(status().isNoContent());
        verify(accountManagementService).withdraw(any(), any());
    }

    @Test
    void withdrawalRequiresAccessToken() throws Exception {
        for (String authorization : new String[]{"", bearer(JwtTokenType.REFRESH)}) {
            mockMvc.perform(delete("/api/account").contextPath("/api")
                            .header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"pw\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private AccountMeResDto accountResponse() {
        return new AccountMeResDto(42L, "회원", UserStatus.ACTIVE, SystemRole.USER,
                List.of(AuthProvider.LOCAL), "member-id", null, null);
    }

    private String bearer(SystemRole role) {
        return bearer(JwtTokenType.ACCESS);
    }

    private String bearer(JwtTokenType tokenType) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("42").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, tokenType.name()).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
