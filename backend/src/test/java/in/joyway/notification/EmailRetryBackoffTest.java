package in.joyway.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmailRetryBackoffTest {

    private final Duration base = Duration.ofMinutes(5);

    @Test
    void firstAttemptIsDueImmediately() {
        assertThat(EmailRetryBackoff.isDue(null, 0, base, Instant.parse("2026-08-28T10:00:00Z")))
                .isTrue();
    }

    @Test
    void waitsBaseDelayAfterFirstFailure() {
        Instant last = Instant.parse("2026-08-28T10:00:00Z");
        Instant tooSoon = last.plus(Duration.ofMinutes(4));
        Instant due = last.plus(Duration.ofMinutes(5));

        assertThat(EmailRetryBackoff.isDue(last, 1, base, tooSoon)).isFalse();
        assertThat(EmailRetryBackoff.isDue(last, 1, base, due)).isTrue();
    }

    @Test
    void doublesDelayOnSubsequentAttempts() {
        assertThat(EmailRetryBackoff.delayAfterAttempts(1, base)).isEqualTo(Duration.ofMinutes(5));
        assertThat(EmailRetryBackoff.delayAfterAttempts(2, base)).isEqualTo(Duration.ofMinutes(10));
        assertThat(EmailRetryBackoff.delayAfterAttempts(3, base)).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void capsExponentialGrowth() {
        Duration capped = EmailRetryBackoff.delayAfterAttempts(20, base);
        assertThat(capped).isEqualTo(base.multipliedBy(1L << 6));
    }
}
