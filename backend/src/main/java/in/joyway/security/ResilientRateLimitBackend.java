package in.joyway.security;

import in.joyway.config.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Redis-backed limiter with category-specific behaviour when Redis is unavailable.
 *
 * <ul>
 *   <li>Admin login: emergency in-memory fallback (same configured limit, per replica) or fail-closed.</li>
 *   <li>Booking/payment buckets: fail-open with {@code security.rate_limit_degraded} logging.</li>
 * </ul>
 */
@Slf4j
public class ResilientRateLimitBackend implements RateLimitBackend {

    private final RedisRateLimitBackend redisBackend;
    private final InMemoryRateLimitBackend emergencyBackend;
    private final RateLimitProperties properties;

    public ResilientRateLimitBackend(RedisRateLimitBackend redisBackend,
                                     InMemoryRateLimitBackend emergencyBackend,
                                     RateLimitProperties properties) {
        this.redisBackend = redisBackend;
        this.emergencyBackend = emergencyBackend;
        this.properties = properties;
    }

    @Override
    public RateLimitResult tryAcquire(RateLimitBucket bucket, String clientKey, int limit, Instant now) {
        try {
            return redisBackend.tryAcquire(bucket, clientKey, limit, now);
        } catch (RuntimeException ex) {
            return handleRedisFailure(bucket, clientKey, limit, now, ex);
        }
    }

    private RateLimitResult handleRedisFailure(RateLimitBucket bucket,
                                               String clientKey,
                                               int limit,
                                               Instant now,
                                               RuntimeException ex) {
        log.warn("event=security.rate_limit_degraded bucket={} client={} reason={}",
                bucket.id(),
                ClientAddressResolver.maskForLog(clientKey),
                ex.getClass().getSimpleName());

        if (bucket.isLogin()) {
            if (properties.isLoginFailClosedOnRedisError()) {
                return RateLimitResult.serviceUnavailable();
            }
            return emergencyBackend.tryAcquire(bucket, clientKey, limit, now);
        }

        return RateLimitResult.degradedAllowed();
    }

    @Override
    public void evictExpired(Instant now) {
        emergencyBackend.evictExpired(now);
    }
}
