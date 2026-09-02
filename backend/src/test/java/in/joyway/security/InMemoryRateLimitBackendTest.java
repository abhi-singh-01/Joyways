package in.joyway.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitBackendTest {

    private final InMemoryRateLimitBackend limiter = new InMemoryRateLimitBackend(Duration.ofMinutes(1));

    @Test
    void allowsRequestsUpToTheLimit() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now).isAllowed()).isTrue();
        }
    }

    @Test
    void rejectsRequestsBeyondTheLimit() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now);
        }

        assertThat(limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now).isAllowed()).isFalse();
    }

    @Test
    void keepsClientsIndependent() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now);
        }

        assertThat(limiter.tryAcquire(RateLimitBucket.LOGIN, "client-b", 5, now).isAllowed()).isTrue();
    }

    @Test
    void keepsBucketsIndependent() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now);
        }

        assertThat(limiter.tryAcquire(RateLimitBucket.PAYMENT, "client-a", 5, now).isAllowed()).isTrue();
    }

    @Test
    void allowsAgainOnceWindowExpires() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now);
        }

        assertThat(limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now.plusSeconds(61)).isAllowed()).isTrue();
    }

    @Test
    void evictsExpiredWindowsSoTheMapDoesNotGrowUnbounded() {
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        limiter.tryAcquire(RateLimitBucket.LOGIN, "client-a", 5, now);
        limiter.tryAcquire(RateLimitBucket.LOGIN, "client-b", 5, now);
        assertThat(limiter.trackedKeys()).isEqualTo(2);

        limiter.evictExpired(now.plusSeconds(120));

        assertThat(limiter.trackedKeys()).isZero();
    }
}
