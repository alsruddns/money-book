package com.moneybook.backend.security.ratelimit;

import com.moneybook.backend.auth.controller.AuthController;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.admin.dto.AdminPasswordResetResponse;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.admin.service.AdminPasswordResetService;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
import com.moneybook.backend.recovery.controller.AccountSecurityController;
import com.moneybook.backend.recovery.controller.PasswordRecoveryController;
import com.moneybook.backend.recovery.dto.SecurityQuestionResponse;
import com.moneybook.backend.recovery.service.PasswordRecoveryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, PasswordRecoveryController.class, AccountSecurityController.class,
        com.moneybook.backend.admin.controller.AdminPasswordResetController.class})
@Import({SecurityConfig.class, JwtConfig.class, RateLimitWebConfig.class, RateLimitInterceptor.class,
        com.moneybook.backend.accountmanagement.PasswordChangeRequiredInterceptor.class,
        RateLimitSecurityControllerTests.ProtectedController.class})
@TestPropertySource(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "security.rate-limit.login-per-minute=1"
})
class RateLimitSecurityControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @MockitoBean private AuthService authService;
    @MockitoBean private PasswordRecoveryService passwordRecoveryService;
    @MockitoBean private AdminPasswordResetService adminPasswordResetService;
    @MockitoBean private SystemAdminAuthorizationProvider systemAdminAuthorizationProvider;

    @org.junit.jupiter.api.BeforeEach
    void configureRecoverySecurityMocks() {
        when(passwordRecoveryService.questions()).thenReturn(java.util.List.of(
                new SecurityQuestionResponse(com.moneybook.backend.enums.SecurityQuestionCode.FAVORITE_FOOD,
                        "가장 좋아하는 음식은 무엇인가요?")));
        when(systemAdminAuthorizationProvider.hasAdminAccess(any())).thenAnswer(invocation -> {
            var authentication = invocation.<org.springframework.security.core.Authentication>getArgument(0);
            if (!(authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwt)) return false;
            String role = jwt.getToken().getClaimAsString("systemRole");
            return "SYSTEM_ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        });
        when(adminPasswordResetService.reset(anyLong(), any())).thenAnswer(invocation -> {
            var authentication = invocation.<org.springframework.security.core.Authentication>getArgument(1);
            String role = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication)
                    .getToken().getClaimAsString("systemRole");
            if (!"SUPER_ADMIN".equals(role)) throw new BusinessException(ErrorCode.SYSTEM_ADMIN_ACCESS_DENIED);
            return new AdminPasswordResetResponse(51L, "temporary", true);
        });
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/test/protected")
        String protectedRoute() { return "ok"; }

        @GetMapping("/test/missing")
        ResponseEntity<Void> missingRoute() { return ResponseEntity.notFound().build(); }
    }

    @Test
    void loginLimitReturns429RetryAfterAndCommonJsonError() throws Exception {
        when(authService.login(any(), anyString(), anyString())).thenReturn(
                new LoginResponse(42L, "Member", "access", "refresh"));

        mockMvc.perform(login())
                .andExpect(status().isOk());
        mockMvc.perform(login())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void securityErrorsAreJsonAndExpectedHeadersArePresent() throws Exception {
        mockMvc.perform(get("/api/test/protected").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/test/protected").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("Permissions-Policy"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
        mockMvc.perform(get("/api/admin/users").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/test/missing").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousPasswordRecoveryApisArePublicButAccountSecurityIsProtected() throws Exception {
        mockMvc.perform(get("/api/auth/security-questions").contextPath("/api"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/security-questions").contextPath("/api")
                        .header("Authorization", "Bearer expired-or-invalid-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/email-verifications/request").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.test\",\"purpose\":\"SIGNUP\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/email-verifications/confirm").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationUid\":1,\"code\":\"123456\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/password-recovery/email/request").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"missing-user\",\"email\":\"missing@example.test\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/password-recovery/security-question/reset").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"missing-user\",\"questionCode\":\"FAVORITE_FOOD\",\"answer\":\"x\",\"newPassword\":\"NewPassword1!\",\"newPasswordConfirm\":\"NewPassword1!\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/password-recovery/recovery-code/reset").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"missing-user\",\"recoveryCode\":\"invalid\",\"newPassword\":\"NewPassword1!\",\"newPasswordConfirm\":\"NewPassword1!\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/account/security").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account/security").contextPath("/api")
                        .header("Authorization", "Bearer expired-or-invalid-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/users/51/password-reset").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPasswordResetRequiresSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users/51/password-reset").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS, "USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/51/password-reset").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS, "SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/51/password-reset").contextPath("/api")
                        .header("Authorization", bearer(JwtTokenType.ACCESS, "SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login() {
        return post("/api/auth/login").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"person\",\"password\":\"correct-password\"}");
    }

    private String bearer(JwtTokenType type) {
        return bearer(type, null);
    }

    private String bearer(JwtTokenType type, String systemRole) {
        Instant now = Instant.now();
        var claimsBuilder = JwtClaimsSet.builder().subject("42").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, type.name()).claim("sid", "test-session");
        if (systemRole != null) claimsBuilder.claim("systemRole", systemRole);
        var claims = claimsBuilder.build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
