package com.moneybook.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "koreaDateTimeProvider")
public class JpaAuditingConfig {

    /** Dashboard date windows and persisted audit timestamps use the same Korea time zone. */
    @Bean
    public DateTimeProvider koreaDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof JwtAuthenticationToken)) {
                return Optional.empty();
            }
            try {
                return Optional.of(Long.valueOf(authentication.getName()));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        };
    }
}
