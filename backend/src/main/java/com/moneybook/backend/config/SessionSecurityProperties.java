package com.moneybook.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Session lifetime policy that can be overridden by the deployment environment. */
@ConfigurationProperties(prefix = "security.session")
public record SessionSecurityProperties(long idleTimeoutHours) {

    public SessionSecurityProperties {
        if (idleTimeoutHours <= 0) {
            throw new IllegalArgumentException("Session idle timeout must be positive");
        }
    }

    public Duration idleTimeout() {
        return Duration.ofHours(idleTimeoutHours);
    }
}
