package com.moneybook.backend.auth.controller;

import com.moneybook.backend.auth.dto.CurrentUserResponse;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshResponse;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, AuthControllerTests.ProtectedController.class})
@TestPropertySource(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @RestController
    static class ProtectedController {
        @GetMapping("/test/protected")
        String protectedResource() {
            return "ok";
        }
    }

    @Test
    void signUpIsAvailableWithoutAuthenticationOrCsrfToken() throws Exception {
        when(authService.signUp(any())).thenReturn(new SignUpResDto(42L, "닉네임"));

        mockMvc.perform(post("/api/auth/signup").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"new-user","password":"password123",
                                 "passwordConfirm":"password123","nickname":"닉네임"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userUid").value(42))
                .andExpect(jsonPath("$.nickname").value("닉네임"));
    }

    @Test
    void signUpRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void signUpReturnsConflictForDuplicateLoginId() throws Exception {
        when(authService.signUp(any())).thenThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

        mockMvc.perform(post("/api/auth/signup").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"taken","password":"password123",
                                 "passwordConfirm":"password123","nickname":"닉네임"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    void loginIsAvailableWithoutAuthenticationOrCsrfToken() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse(
                42L, "닉네임", "signed-access-token", "signed-refresh-token"));

        mockMvc.perform(post("/api/auth/login").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"member","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userUid").value(42))
                .andExpect(jsonPath("$.nickname").value("닉네임"))
                .andExpect(jsonPath("$.accessToken").value("signed-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("signed-refresh-token"));
    }

    @Test
    void loginRejectsMissingCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void refreshIsAvailableWithoutAuthenticationOrCsrfToken() throws Exception {
        when(authService.refresh(any())).thenReturn(new RefreshResponse("new-access-token"));

        mockMvc.perform(post("/api/auth/refresh").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"signed-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void refreshRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void refreshTokenCannotAuthenticateProtectedApi() throws Exception {
        String refreshToken = token(JwtTokenType.REFRESH);
        String accessToken = token(JwtTokenType.ACCESS);

        mockMvc.perform(get("/api/test/protected").contextPath("/api")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/test/protected").contextPath("/api")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void meReturnsCurrentUserWithAccessToken() throws Exception {
        when(authService.currentUser(any())).thenReturn(new CurrentUserResponse(42L, "닉네임",
                UserStatus.ACTIVE, com.moneybook.backend.enums.SystemRole.USER));

        mockMvc.perform(get("/api/auth/me").contextPath("/api")
                        .header("Authorization", "Bearer " + token(JwtTokenType.ACCESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userUid").value(42))
                .andExpect(jsonPath("$.nickname").value("닉네임"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authService).currentUser(captor.capture());
        assertEquals("42", captor.getValue().getName());
    }

    @Test
    void meRequiresAccessToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").contextPath("/api")
                        .header("Authorization", "Bearer " + token(JwtTokenType.REFRESH)))
                .andExpect(status().isUnauthorized());
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
