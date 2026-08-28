package in.joyway.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final RateLimiter limiter = new RateLimiter(Duration.ofMinutes(1));

    @Test
    void allowsRequestsUpToTheLimit() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("client-a", 5, now)).isTrue();
        }
    }

    @Test
    void rejectsRequestsBeyondTheLimit() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("client-a", 5, now);
        }

        assertThat(limiter.tryAcquire("client-a", 5, now)).isFalse();
    }

    @Test
    void keepsClientsIndependent() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("client-a", 5, now);
        }

        assertThat(limiter.tryAcquire("client-b", 5, now)).isTrue();
    }

    @Test
    void allowsAgainOnceWindowExpires() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("client-a", 5, now);
        }

        assertThat(limiter.tryAcquire("client-a", 5, now.plusSeconds(61))).isTrue();
    }

    @Test
    void evictsExpiredWindowsSoTheMapDoesNotGrowUnbounded() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        limiter.tryAcquire("client-a", 5, now);
        limiter.tryAcquire("client-b", 5, now);
        assertThat(limiter.trackedKeys()).isEqualTo(2);

        limiter.evictExpired(now.plusSeconds(120));

        assertThat(limiter.trackedKeys()).isZero();
    }
}
