package in.joyway.notification;

import java.time.Duration;
import java.time.Instant;

/**
 * Exponential backoff for SMTP retries. Attempt 1 waits {@code base}, attempt 2 waits
 * {@code 2 * base}, attempt 3 waits {@code 4 * base}, capped at 6 doublings so a
 * misconfigured clock cannot produce an unbounded delay.
 */
public final class EmailRetryBackoff {

    private static final int MAX_DOUBLINGS = 6;

    private EmailRetryBackoff() {
    }

    public static Duration delayAfterAttempts(int attemptCount, Duration base) {
        int doublings = Math.max(0, attemptCount - 1);
        doublings = Math.min(doublings, MAX_DOUBLINGS);
        return base.multipliedBy(1L << doublings);
    }

    public static boolean isDue(Instant lastAttemptedAt, int attemptCount, Duration base, Instant now) {
        if (lastAttemptedAt == null) {
            return true;
        }
        return !now.isBefore(lastAttemptedAt.plus(delayAfterAttempts(attemptCount, base)));
    }
}
