package com.moneybook.backend.security.ratelimit;

import com.moneybook.backend.auth.controller.AuthController;
import com.moneybook.backend.auth.dto.LoginResponse;
import com.moneybook.backend.auth.service.AuthService;
import com.moneybook.backend.config.JwtConfig;
import com.moneybook.backend.config.SecurityConfig;
import com.moneybook.backend.enums.JwtTokenType;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, RateLimitWebConfig.class, RateLimitInterceptor.class,
        RateLimitSecurityControllerTests.ProtectedController.class})
@TestPropertySource(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "security.rate-limit.login-per-minute=1"
})
class RateLimitSecurityControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @MockitoBean private AuthService authService;

    @RestController
    static class ProtectedController {
        @GetMapping("/test/protected")
        String protectedRoute() { return "ok"; }
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
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login() {
        return post("/api/auth/login").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"person\",\"password\":\"correct-password\"}");
    }

    private String bearer(JwtTokenType type) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("42").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim(JwtTokenType.CLAIM_NAME, type.name()).claim("sid", "test-session").build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
