package com.devhub.auth.service;

import com.devhub.common.error.TooManyRequestsException;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limiter kept in memory: enough for the single Render instance,
 * counters reset on restart and are not shared between instances.
 */
@ApplicationScoped
public class RateLimiter {

    private record Window(Instant resetsAt, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** Counts one hit on {@code key}; throws 429 once {@code limit} hits are exceeded within {@code window}. */
    public void check(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Window current = windows.compute(key, (k, w) -> w == null || !now.isBefore(w.resetsAt())
                ? new Window(now.plus(window), 1)
                : new Window(w.resetsAt(), w.count() + 1));

        if (current.count() > limit) {
            throw new TooManyRequestsException("TOO_MANY_ATTEMPTS", Duration.between(now, current.resetsAt()));
        }
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Test hook: forget every counter. */
    public void clear() {
        windows.clear();
    }

    @Scheduled(every = "10m")
    void evictExpired() {
        Instant now = Instant.now();
        windows.values().removeIf(w -> !now.isBefore(w.resetsAt()));
    }
}
