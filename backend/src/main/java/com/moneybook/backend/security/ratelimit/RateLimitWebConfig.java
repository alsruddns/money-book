package com.moneybook.backend.security.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@Import(InMemoryRateLimiter.class)
public class RateLimitWebConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor interceptor;
    private final com.moneybook.backend.accountmanagement.PasswordChangeRequiredInterceptor passwordChangeInterceptor;

    public RateLimitWebConfig(RateLimitInterceptor interceptor,
            com.moneybook.backend.accountmanagement.PasswordChangeRequiredInterceptor passwordChangeInterceptor) {
        this.interceptor = interceptor;
        this.passwordChangeInterceptor = passwordChangeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
        registry.addInterceptor(passwordChangeInterceptor);
    }
}
