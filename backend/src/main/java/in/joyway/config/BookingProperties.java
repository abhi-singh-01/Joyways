package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@ConfigurationProperties(prefix = "joyway.booking")
@Getter
@Setter
public class BookingProperties {

    /** Unpaid bookings older than this are expired and seats released. */
    private int paymentWindowMinutes = 30;

    /**
     * Minutes before expiry to email an unpaid reminder.
     * Clamped below the payment window so a reminder interval always exists.
     */
    private int paymentReminderMinutes = 10;

    public Instant paymentExpiresAt(Instant createdAt) {
        if (createdAt == null) {
            return null;
        }
        return createdAt.plus(paymentWindowMinutes, ChronoUnit.MINUTES);
    }

    public int effectiveReminderMinutes() {
        int maxReminder = paymentWindowMinutes > 1 ? paymentWindowMinutes - 1 : 1;
        return Math.clamp(paymentReminderMinutes, 1, maxReminder);
    }
}
