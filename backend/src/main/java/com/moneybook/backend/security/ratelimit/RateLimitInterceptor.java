package com.moneybook.backend.security.ratelimit;

import com.moneybook.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

/** Applies endpoint-specific quotas after authentication and before controller execution. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Pattern ADMIN_MUTATION = Pattern.compile(
            "/admin/users/[0-9]+/(status|system-role|sessions/revoke-all|password-reset)");
    private final RateLimiter limiter;
    private final RateLimitProperties properties;

    public RateLimitInterceptor(RateLimiter limiter, RateLimitProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.startsWith(contextPath)
                ? requestUri.substring(contextPath.length()) : request.getServletPath();
        String method = request.getMethod();
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String user = authenticatedUserKey();
        if (path.equals("/board/posts") && HttpMethod.POST.matches(method)) {
            return allow(response, "board-post-user-minute", user, 10, Duration.ofMinutes(1));
        }
        if (path.matches("/board/posts/[0-9]+/comments") && HttpMethod.POST.matches(method)) {
            return allow(response, "board-comment-user-minute", user, 30, Duration.ofMinutes(1));
        }
        if (isSensitiveEndpoint(method, path)) response.setHeader("Cache-Control", "no-store");

        if (path.equals("/auth/email-verifications/request") && HttpMethod.POST.matches(method))
            return allow(response,"recovery-email-send-ip-hour",ip,5,Duration.ofHours(1));
        if (path.equals("/auth/password-recovery/email/request") && HttpMethod.POST.matches(method))
            return allow(response,"recovery-email-request-ip-hour",ip,5,Duration.ofHours(1));
        if (path.equals("/auth/email-verifications/confirm") && HttpMethod.POST.matches(method))
            return allow(response,"recovery-email-check-ip-15m",ip,10,Duration.ofMinutes(15));
        if (path.startsWith("/auth/password-recovery/") && HttpMethod.POST.matches(method))
            return allow(response,"password-recovery-ip-15m",ip,5,Duration.ofMinutes(15));
        if (path.equals("/account/security/recovery-codes/regenerate") && HttpMethod.POST.matches(method))
            return allow(response,"recovery-code-regenerate-user-10m",user,5,Duration.ofMinutes(10));
        if (path.startsWith("/account/security/") && (HttpMethod.PUT.matches(method)||HttpMethod.PATCH.matches(method)||HttpMethod.DELETE.matches(method)))
            return allow(response,"account-security-user-10m",user,10,Duration.ofMinutes(10));

        if (is(method, path, HttpMethod.POST, "/auth/login")) {
            return allow(response, "login-ip-minute", ip, properties.getLoginPerMinute(), Duration.ofMinutes(1))
                    && allow(response, "login-ip-15m", ip, properties.getLoginPer15Minutes(), Duration.ofMinutes(15));
        }
        if (is(method, path, HttpMethod.POST, "/auth/signup")) {
            return allow(response, "signup-ip-hour", ip, properties.getSignupPerHour(), Duration.ofHours(1));
        }
        if (is(method, path, HttpMethod.POST, "/auth/refresh")) {
            return allow(response, "refresh-ip-minute", ip, properties.getRefreshPerMinute(), Duration.ofMinutes(1));
        }
        if (is(method, path, HttpMethod.POST, "/auth/logout")) {
            return allow(response, "logout-user-minute", user, properties.getLogoutPerMinute(), Duration.ofMinutes(1));
        }
        if (is(method, path, HttpMethod.PATCH, "/account/password")) {
            return allow(response, "password-user-10m", user,
                    properties.getPasswordChangePer10Minutes(), Duration.ofMinutes(10));
        }
        if (is(method, path, HttpMethod.DELETE, "/account")) {
            return allow(response, "withdraw-user-10m", user,
                    properties.getWithdrawalPer10Minutes(), Duration.ofMinutes(10));
        }
        if (is(method, path, HttpMethod.POST, "/account/sessions/logout-all")) {
            return allow(response, "logout-all-user-minute", user,
                    properties.getLogoutAllPerMinute(), Duration.ofMinutes(1));
        }
        if (ADMIN_MUTATION.matcher(path).matches()
                && (HttpMethod.PATCH.matches(method) || HttpMethod.POST.matches(method))) {
            return allow(response, "admin-mutation-user-minute", user,
                    properties.getAdminMutationPerMinute(), Duration.ofMinutes(1));
        }
        return true;
    }

    private boolean allow(HttpServletResponse response, String policy, String key, int limit, Duration window)
            throws IOException {
        RateLimitDecision decision = limiter.tryAcquire(policy, key, limit, window);
        if (decision.allowed()) return true;
        response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getStatus().value());
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setHeader("Cache-Control", "no-store");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + ErrorCode.RATE_LIMIT_EXCEEDED.name()
                + "\",\"message\":\"" + ErrorCode.RATE_LIMIT_EXCEEDED.getMessage() + "\"}");
        return false;
    }

    private String authenticatedUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? "unauthenticated" : authentication.getName();
    }

    private boolean is(String method, String actualPath, HttpMethod expected, String expectedPath) {
        return expected.matches(method) && expectedPath.equals(actualPath);
    }

    private boolean isSensitiveEndpoint(String method, String path) {
        return (HttpMethod.POST.matches(method) && (path.startsWith("/auth/") || path.equals("/account/sessions/logout-all")))
                || (HttpMethod.PATCH.matches(method) && path.equals("/account/password"))
                || path.equals("/account/security") || path.startsWith("/account/security/")
                || path.startsWith("/auth/password-recovery/")
                || path.startsWith("/auth/email-verifications/")
                || ((HttpMethod.PATCH.matches(method) || HttpMethod.POST.matches(method))
                && ADMIN_MUTATION.matcher(path).matches())
                || (HttpMethod.DELETE.matches(method) && path.equals("/account"));
    }
}
