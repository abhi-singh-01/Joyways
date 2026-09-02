package in.joyway.security;

import in.joyway.config.RateLimitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Instant;
import java.util.List;

/**
 * Deployment-wide fixed-window counter backed by Redis {@code INCR}.
 *
 * <p>Atomicity: a Lua script performs {@code INCR} and sets {@code EXPIRE} only when the counter
 * transitions from absent to {@code 1}. Subsequent requests in the same window only increment;
 * they never shorten or delete the key, so concurrent requests cannot race TTL away from each other.
 *
 * <p>Window: one minute per {@code epochMinute} key suffix. TTL ({@link RateLimitProperties#getWindowTtlSeconds()})
 * is longer than 60 seconds so keys expire naturally after the minute boundary without manual deletion.
 */
public class RedisRateLimitBackend implements RateLimitBackend {

    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>(
            """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                      redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    return count
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RedisRateLimitBackend(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public RateLimitResult tryAcquire(RateLimitBucket bucket, String clientKey, int limit, Instant now) {
        String key = RateLimitKeyBuilder.buildKey(properties, bucket, clientKey, now);
        Long count = redisTemplate.execute(
                INCR_WITH_EXPIRE,
                List.of(key),
                String.valueOf(properties.getWindowTtlSeconds())
        );
        if (count == null) {
            throw new IllegalStateException("Redis rate limit script returned null.");
        }
        return count <= limit ? RateLimitResult.permitted() : RateLimitResult.rejected();
    }
}
