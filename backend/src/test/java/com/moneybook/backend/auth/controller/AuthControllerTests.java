package com.moneybook.backend.auth.controller;

import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.dto.RefreshResponse;
import com.moneybook.backend.auth.dto.SignUpResDto;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

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
}
