package in.joyway.security;

import java.time.Instant;

/**
 * Shared fixed-window rate limit counter. Implementations may be in-memory (per replica)
 * or Redis-backed (deployment-wide).
 */
public interface RateLimitBackend {

    /**
     * Attempts to consume one request against the configured limit.
     *
     * <p>Uses a fixed one-minute window keyed by {@code bucket}, {@code clientKey}, and the
     * current epoch minute.
     */
    RateLimitResult tryAcquire(RateLimitBucket bucket, String clientKey, int limit, Instant now);

    /** Optional cleanup hook for in-memory backends. */
    default void evictExpired(Instant now) {
        // no-op for Redis backends
    }
}
