package com.moneybook.backend.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTests {
    private final RateLimiter limiter = mock(RateLimiter.class);
    private final RateLimitProperties properties = new RateLimitProperties();
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(limiter, properties);

    @BeforeEach
    void authenticateSensitiveRoutes() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user-42", "", List.of()));
        when(limiter.tryAcquire(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new RateLimitDecision(true, 0));
    }

    @Test
    void everySensitiveEndpointUsesItsConfiguredKeyAndPolicy() throws Exception {
        assertPolicy("POST", "/auth/signup", "signup-ip-hour", "192.0.2.10",
                properties.getSignupPerHour(), Duration.ofHours(1));
        assertPolicy("POST", "/auth/refresh", "refresh-ip-minute", "192.0.2.10",
                properties.getRefreshPerMinute(), Duration.ofMinutes(1));
        assertPolicy("POST", "/auth/logout", "logout-user-minute", "user-42",
                properties.getLogoutPerMinute(), Duration.ofMinutes(1));
        assertPolicy("PATCH", "/account/password", "password-user-10m", "user-42",
                properties.getPasswordChangePer10Minutes(), Duration.ofMinutes(10));
        assertPolicy("DELETE", "/account", "withdraw-user-10m", "user-42",
                properties.getWithdrawalPer10Minutes(), Duration.ofMinutes(10));
        assertPolicy("POST", "/account/sessions/logout-all", "logout-all-user-minute", "user-42",
                properties.getLogoutAllPerMinute(), Duration.ofMinutes(1));
        assertPolicy("POST", "/board/posts", "board-post-user-minute", "user-42",
                10, Duration.ofMinutes(1));
        assertPolicy("POST", "/board/posts/88/comments", "board-comment-user-minute", "user-42",
                30, Duration.ofMinutes(1));
        assertPolicy("PATCH", "/admin/users/7/status", "admin-mutation-user-minute", "user-42",
                properties.getAdminMutationPerMinute(), Duration.ofMinutes(1));
        assertPolicy("PATCH", "/admin/users/7/system-role", "admin-mutation-user-minute", "user-42",
                properties.getAdminMutationPerMinute(), Duration.ofMinutes(1));
        assertPolicy("POST", "/admin/users/7/sessions/revoke-all", "admin-mutation-user-minute", "user-42",
                properties.getAdminMutationPerMinute(), Duration.ofMinutes(1));
        assertPolicy("POST", "/admin/users/7/password-reset", "admin-mutation-user-minute", "user-42",
                properties.getAdminMutationPerMinute(), Duration.ofMinutes(1));
        assertPolicy("POST", "/auth/email-verifications/request", "recovery-email-send-ip-hour", "192.0.2.10",
                5, Duration.ofHours(1));
        assertPolicy("POST", "/auth/email-verifications/confirm", "recovery-email-check-ip-15m", "192.0.2.10",
                10, Duration.ofMinutes(15));
        assertPolicy("POST", "/auth/password-recovery/email/request", "recovery-email-request-ip-hour", "192.0.2.10",
                5, Duration.ofHours(1));
        assertPolicy("POST", "/auth/password-recovery/security-question/reset", "password-recovery-ip-15m", "192.0.2.10",
                5, Duration.ofMinutes(15));
        assertPolicy("POST", "/account/security/recovery-codes/regenerate", "recovery-code-regenerate-user-10m", "user-42",
                5, Duration.ofMinutes(10));

        clearInvocations(limiter);
        MockHttpServletRequest login = request("POST", "/auth/login");
        assertTrue(interceptor.preHandle(login, new MockHttpServletResponse(), new Object()));
        verify(limiter).tryAcquire(eq("login-ip-minute"), eq("192.0.2.10"),
                eq(properties.getLoginPerMinute()), eq(Duration.ofMinutes(1)));
        verify(limiter).tryAcquire(eq("login-ip-15m"), eq("192.0.2.10"),
                eq(properties.getLoginPer15Minutes()), eq(Duration.ofMinutes(15)));
    }

    private void assertPolicy(String method, String path, String policy, String key,
                              int limit, Duration window) throws Exception {
        clearInvocations(limiter);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request(method, path), response, new Object()));
        verify(limiter).tryAcquire(eq(policy), eq(key), eq(limit), eq(window));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api" + path);
        request.setContextPath("/api");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
