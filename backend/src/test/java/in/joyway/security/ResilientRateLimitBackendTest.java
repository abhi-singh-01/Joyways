package in.joyway.security;

import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientRateLimitBackendTest {

    @Test
    void bookingBucketFailsOpenWhenRedisUnavailable() {
        RateLimitProperties properties = new RateLimitProperties();
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(Duration.ofMinutes(1));
        RedisRateLimitBackend failingRedis = failingRedisBackend();

        ResilientRateLimitBackend backend = new ResilientRateLimitBackend(failingRedis, emergency, properties);
        Instant now = Instant.parse("2026-09-02T12:10:00Z");

        RateLimitResult result = backend.tryAcquire(RateLimitBucket.BOOKING_CREATE, "203.0.113.5", 20, now);

        assertThat(result.outcome()).isEqualTo(RateLimitOutcome.DEGRADED_ALLOWED);
    }

    @Test
    void paymentBucketFailsOpenWhenRedisUnavailable() {
        RateLimitProperties properties = new RateLimitProperties();
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(Duration.ofMinutes(1));
        ResilientRateLimitBackend backend = new ResilientRateLimitBackend(
                failingRedisBackend(), emergency, properties);

        RateLimitResult result = backend.tryAcquire(
                RateLimitBucket.PAYMENT, "203.0.113.6", 30, Instant.parse("2026-09-02T12:11:00Z"));

        assertThat(result.outcome()).isEqualTo(RateLimitOutcome.DEGRADED_ALLOWED);
    }

    @Test
    void loginUsesEmergencyLimiterWhenRedisUnavailable() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setLoginFailClosedOnRedisError(false);
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(Duration.ofMinutes(1));
        ResilientRateLimitBackend backend = new ResilientRateLimitBackend(
                failingRedisBackend(), emergency, properties);
        Instant now = Instant.parse("2026-09-02T12:12:00Z");

        for (int i = 0; i < 10; i++) {
            assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.7", 10, now).isAllowed()).isTrue();
        }
        assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.7", 10, now).isAllowed()).isFalse();
    }

    @Test
    void loginFailsClosedWhenConfiguredAndRedisUnavailable() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setLoginFailClosedOnRedisError(true);
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(Duration.ofMinutes(1));
        ResilientRateLimitBackend backend = new ResilientRateLimitBackend(
                failingRedisBackend(), emergency, properties);

        RateLimitResult result = backend.tryAcquire(
                RateLimitBucket.LOGIN, "203.0.113.8", 10, Instant.parse("2026-09-02T12:13:00Z"));

        assertThat(result.outcome()).isEqualTo(RateLimitOutcome.SERVICE_UNAVAILABLE);
    }

    private static RedisRateLimitBackend failingRedisBackend() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(), anyList(), anyString()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        return new RedisRateLimitBackend(template, new RateLimitProperties());
    }
}
