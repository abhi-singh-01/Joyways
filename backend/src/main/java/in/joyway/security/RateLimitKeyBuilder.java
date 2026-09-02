package in.joyway.security;

import in.joyway.config.RateLimitProperties;

import java.time.Instant;

/**
 * Builds Redis keys for the fixed-window rate limiter.
 *
 * <p>Format: {@code {prefix}:{bucket}:{normalizedClientIp}:{epochMinute}}
 */
public final class RateLimitKeyBuilder {

    private RateLimitKeyBuilder() {
    }

    public static String buildKey(RateLimitProperties properties, RateLimitBucket bucket,
                                  String clientKey, Instant now) {
        long epochMinute = now.getEpochSecond() / 60L;
        return properties.getRedisKeyPrefix()
                + ":"
                + bucket.id()
                + ":"
                + clientKey
                + ":"
                + epochMinute;
    }
}
