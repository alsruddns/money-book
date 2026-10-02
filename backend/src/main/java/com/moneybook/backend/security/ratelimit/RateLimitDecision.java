package com.moneybook.backend.security.ratelimit;

/** Result for one sliding-window quota check. Retry is zero for an accepted request. */
public record RateLimitDecision(boolean allowed, long retryAfterSeconds) { }
