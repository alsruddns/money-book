package com.moneybook.backend.security.ratelimit;


import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe sliding-window limiter with lazy TTL cleanup and a hard key cap. */
public class InMemoryRateLimiter implements RateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Object keyLock = new Object();
    private final AtomicLong calls = new AtomicLong();
    private final int maximumKeys;

    public InMemoryRateLimiter(RateLimitProperties properties) {
        this.maximumKeys = properties.getMaximumKeys();
    }

    @Override
    public RateLimitDecision tryAcquire(String policy, String key, int limit, Duration duration) {
        if (policy == null || key == null || key.isBlank() || limit < 1
                || duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("A valid rate limit policy is required");
        }
        long now = System.currentTimeMillis();
        String compositeKey = policy + ':' + key;
        Window window = getWindow(compositeKey, now, duration.toMillis());
        if (window == null) return new RateLimitDecision(false, capacityRetryAfterSeconds(now));

        RateLimitDecision decision;
        synchronized (window) {
            long cutoff = now - window.durationMillis;
            while (!window.events.isEmpty() && window.events.peekFirst() <= cutoff) {
                window.events.removeFirst();
            }
            window.lastAccessMillis = now;
            if (window.events.size() >= limit) {
                long waitMillis = window.events.peekFirst() + window.durationMillis - now;
                decision = new RateLimitDecision(false, Math.max(1, (waitMillis + 999) / 1000));
            } else {
                window.events.addLast(now);
                decision = new RateLimitDecision(true, 0);
            }
        }
        if (calls.incrementAndGet() % 256 == 0) cleanup(now);
        return decision;
    }

    int trackedKeyCount() {
        return windows.size();
    }

    private Window getWindow(String key, long now, long durationMillis) {
        Window found = windows.get(key);
        if (found != null) {
            found.lastAccessMillis = now;
            return found;
        }
        synchronized (keyLock) {
            found = windows.get(key);
            if (found != null) {
                found.lastAccessMillis = now;
                return found;
            }
            cleanupLocked(now);
            if (windows.size() >= maximumKeys) return null;
            Window created = new Window(now, durationMillis);
            windows.put(key, created);
            return created;
        }
    }

    private void cleanup(long now) {
        synchronized (keyLock) {
            cleanupLocked(now);
        }
    }

    private void cleanupLocked(long now) {
        windows.entrySet().removeIf(entry -> entry.getValue().lastAccessMillis
                < now - entry.getValue().durationMillis);
    }

    private long capacityRetryAfterSeconds(long now) {
        synchronized (keyLock) {
            long nextExpiry = windows.values().stream()
                    .mapToLong(window -> window.lastAccessMillis + window.durationMillis)
                    .min()
                    .orElse(now + 1000);
            long waitMillis = nextExpiry - now;
            return Math.max(1, (waitMillis + 999) / 1000);
        }
    }

    private static final class Window {
        private final ArrayDeque<Long> events = new ArrayDeque<>();
        private final long durationMillis;
        private volatile long lastAccessMillis;

        private Window(long now, long durationMillis) {
            this.lastAccessMillis = now;
            this.durationMillis = durationMillis;
        }
    }
}
