package com.moneybook.backend.security.ratelimit;

import java.time.Duration;

public interface RateLimiter {
    RateLimitDecision tryAcquire(String policy, String key, int limit, Duration window);
}
