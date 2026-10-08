package com.moneybook.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SessionSecurityProperties.class)
public class SessionSecurityConfig {

    @Bean
    public Clock sessionClock() {
        return Clock.systemUTC();
    }
}
