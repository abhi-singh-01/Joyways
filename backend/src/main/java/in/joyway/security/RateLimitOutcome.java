package in.joyway.security;

/**
 * Result classification for a rate-limit decision.
 */
public enum RateLimitOutcome {
    /** Within limit on the primary backend. */
    ALLOWED,
    /** Limit exceeded. */
    REJECTED,
    /** Allowed because a non-login bucket degraded when Redis was unavailable. */
    DEGRADED_ALLOWED,
    /** Login rejected because Redis was unavailable and fail-closed policy applies. */
    SERVICE_UNAVAILABLE
}
