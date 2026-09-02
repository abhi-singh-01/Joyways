package in.joyway.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window in-memory request counter.
 *
 * <p>State is per JVM instance. Used for local development and as an emergency fallback for
 * admin login when Redis is unavailable.
 */
public class InMemoryRateLimitBackend implements RateLimitBackend {

    private final Duration window;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitBackend(Duration window) {
        this.window = window;
    }

    @Override
    public RateLimitResult tryAcquire(RateLimitBucket bucket, String clientKey, int limit, Instant now) {
        String storageKey = bucket.id() + "|" + clientKey + "|" + epochMinute(now);
        Window current = windows.compute(storageKey, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt)) {
                return new Window(now.plus(window));
            }
            return existing;
        });
        return current.count.incrementAndGet() <= limit
                ? RateLimitResult.permitted()
                : RateLimitResult.rejected();
    }

    @Override
    public void evictExpired(Instant now) {
        windows.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }

    int trackedKeys() {
        return windows.size();
    }

    private static long epochMinute(Instant now) {
        return now.getEpochSecond() / 60L;
    }

    private static final class Window {
        private final Instant expiresAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
