package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;

    /** {@code memory} for local dev; {@code redis} for production multi-replica enforcement. */
    private String backend = "memory";

    /** Admin login attempts allowed per client per minute. */
    private int loginRequestsPerMinute = 10;

    /** Booking creation attempts allowed per client per minute. */
    private int bookingRequestsPerMinute = 20;

    /** Lookup, receipt, preview, and cancel attempts allowed per client per minute. */
    private int bookingLookupRequestsPerMinute = 10;

    /** Payment order/verify attempts allowed per client per minute. */
    private int paymentRequestsPerMinute = 30;

    private String redisKeyPrefix = "joyway:rl:v1";

    /** TTL applied on first INCR in a window; must exceed 60 seconds for one-minute windows. */
    private int windowTtlSeconds = 120;

    private String redisHost = "localhost";

    private int redisPort = 6379;

    private String redisPassword = "";

    private boolean redisSsl = false;

    private int redisCommandTimeoutMs = 100;

    /**
     * When Redis is unavailable, reject admin login with 503 instead of using the emergency
     * in-memory fallback.
     */
    private boolean loginFailClosedOnRedisError = false;

    public boolean isRedisBackend() {
        return "redis".equalsIgnoreCase(backend);
    }
}
