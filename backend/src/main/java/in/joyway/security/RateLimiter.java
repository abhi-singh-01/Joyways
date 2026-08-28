package in.joyway.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window in-memory request counter.
 *
 * <p>State is per instance, so with multiple replicas the effective limit is
 * {@code limit x replicas}. That is acceptable for the goal here, which is blunting
 * credential stuffing and scripted abuse rather than precise quota enforcement.
 */
public class RateLimiter {

    private final Duration window;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(Duration window) {
        this.window = window;
    }

    /**
     * @return {@code true} when the request is within the limit, {@code false} when it
     *         should be rejected.
     */
    public boolean tryAcquire(String key, int limit, Instant now) {
        Window current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt)) {
                return new Window(now.plus(window));
            }
            return existing;
        });
        return current.count.incrementAndGet() <= limit;
    }

    /**
     * Drops expired entries so the map cannot grow without bound from one-off clients.
     */
    public void evictExpired(Instant now) {
        windows.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }

    int trackedKeys() {
        return windows.size();
    }

    private static final class Window {
        private final Instant expiresAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
