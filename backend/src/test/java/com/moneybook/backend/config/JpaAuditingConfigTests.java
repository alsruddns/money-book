package com.moneybook.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JpaAuditingConfigTests {

    private final JpaAuditingConfig config = new JpaAuditingConfig();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedUserUidAsAuditor() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "42")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals(Optional.of(42L), config.auditorAware().getCurrentAuditor());
    }

    @Test
    void anonymousRequestHasNoAuditor() {
        assertEquals(Optional.empty(), config.auditorAware().getCurrentAuditor());
    }
}
