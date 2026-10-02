package com.moneybook.backend.security.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTests {

    @Test
    void enforcesSlidingWindowAndSeparatesKeysAndPolicies() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new RateLimitProperties());

        assertTrue(limiter.tryAcquire("login", "ip-a", 2, Duration.ofSeconds(1)).allowed());
        assertTrue(limiter.tryAcquire("login", "ip-a", 2, Duration.ofSeconds(1)).allowed());
        RateLimitDecision blocked = limiter.tryAcquire("login", "ip-a", 2, Duration.ofSeconds(1));
        assertFalse(blocked.allowed());
        assertTrue(blocked.retryAfterSeconds() >= 1);
        assertTrue(limiter.tryAcquire("login", "ip-b", 2, Duration.ofSeconds(1)).allowed());
        assertTrue(limiter.tryAcquire("signup", "ip-a", 2, Duration.ofSeconds(1)).allowed());
    }

    @Test
    void expiredWindowAllowsRequestsAgainAndIdleKeysAreCleaned() throws Exception {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new RateLimitProperties());
        assertTrue(limiter.tryAcquire("short", "client", 1, Duration.ofMillis(30)).allowed());
        assertFalse(limiter.tryAcquire("short", "client", 1, Duration.ofMillis(30)).allowed());
        Thread.sleep(60);
        limiter.tryAcquire("short", "new-client", 1, Duration.ofMillis(30));
        assertEquals(1, limiter.trackedKeyCount());
        assertTrue(limiter.tryAcquire("short", "client", 1, Duration.ofMillis(30)).allowed());
    }

    @Test
    void simultaneousRequestsCannotExceedTheConfiguredLimit() throws Exception {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(new RateLimitProperties());
        int threads = 32;
        var executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger accepted = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                try {
                    start.await();
                    if (limiter.tryAcquire("parallel", "same-user", 4, Duration.ofMinutes(1)).allowed()) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        executor.shutdownNow();

        assertEquals(4, accepted.get());
    }

    @Test
    void maximumKeyCountPreventsUnboundedMemoryGrowth() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaximumKeys(2);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);

        assertTrue(limiter.tryAcquire("p", "one", 1, Duration.ofHours(1)).allowed());
        assertTrue(limiter.tryAcquire("p", "two", 1, Duration.ofHours(1)).allowed());
        assertFalse(limiter.tryAcquire("p", "three", 1, Duration.ofHours(1)).allowed());
        assertEquals(2, limiter.trackedKeyCount());
    }
}
