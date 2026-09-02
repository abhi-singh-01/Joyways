package in.joyway.security;

/**
 * Shared rate-limit buckets. Limits are configured per bucket in {@link in.joyway.config.RateLimitProperties}.
 */
public enum RateLimitBucket {
    LOGIN("login"),
    BOOKING_CREATE("booking-create"),
    BOOKING_ACCESS("booking-access"),
    PAYMENT("payment");

    private final String id;

    RateLimitBucket(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isLogin() {
        return this == LOGIN;
    }
}
