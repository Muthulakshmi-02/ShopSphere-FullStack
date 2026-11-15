package com.example.MyProject.Models;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts attempts per key (for example "login:email:a@b.com" or "login:ip:1.2.3.4") inside a time
 * window. Used to slow down password guessing and to stop reset-email spam.
 *
 * It lives in memory, so it is right for ONE running copy of the backend. If you later run several
 * copies behind a load balancer, move the counters to Redis (or use Bucket4j) so they are shared.
 */
@Component
public class AttemptLimiter {

    // A key never grows past this size: an attacker sending thousands of different emails cannot
    // make the map eat all the memory.
    private static final int MAX_KEYS = 50_000;

    private record Bucket(int count, Instant expiresAt) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Records one attempt for this key and returns how many there have been in the current window. */
    public int hit(String key, Duration window) {
        if (buckets.size() > MAX_KEYS) {
            cleanup();
        }
        Instant now = Instant.now();
        return buckets.compute(key, (k, bucket) -> {
            if (bucket == null || !bucket.expiresAt().isAfter(now)) {
                return new Bucket(1, now.plus(window));      // first attempt of a new window
            }
            return new Bucket(bucket.count() + 1, bucket.expiresAt());
        }).count();
    }

    /** True when this key has already reached {@code max} attempts in the current window. */
    public boolean isLimited(String key, int max) {
        Bucket bucket = buckets.get(key);
        return bucket != null && bucket.expiresAt().isAfter(Instant.now()) && bucket.count() >= max;
    }

    /** Seconds until this key's window ends (0 if there is none). */
    public long secondsLeft(String key) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) return 0;
        long seconds = Duration.between(Instant.now(), bucket.expiresAt()).getSeconds();
        return Math.max(0, seconds);
    }

    /** Forget this key (for example after a successful login). */
    public void reset(String key) {
        buckets.remove(key);
    }

    /** Removes windows that have ended. Requires @EnableScheduling (you already have it). */
    @Scheduled(fixedDelay = 600_000)
    public void cleanup() {
        Instant now = Instant.now();
        buckets.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
    }
}