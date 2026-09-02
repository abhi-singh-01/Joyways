package in.joyway.security;

/**
 * Rate-limit decision for a single request.
 */
public record RateLimitResult(RateLimitOutcome outcome) {

    public boolean isAllowed() {
        return outcome == RateLimitOutcome.ALLOWED
                || outcome == RateLimitOutcome.DEGRADED_ALLOWED;
    }

    public static RateLimitResult permitted() {
        return new RateLimitResult(RateLimitOutcome.ALLOWED);
    }

    public static RateLimitResult rejected() {
        return new RateLimitResult(RateLimitOutcome.REJECTED);
    }

    public static RateLimitResult degradedAllowed() {
        return new RateLimitResult(RateLimitOutcome.DEGRADED_ALLOWED);
    }

    public static RateLimitResult serviceUnavailable() {
        return new RateLimitResult(RateLimitOutcome.SERVICE_UNAVAILABLE);
    }
}
