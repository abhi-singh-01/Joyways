package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.mail")
@Getter
@Setter
public class MailProperties {
    /** Legacy fallback; mirrors general sender. */
    private String from = "hello@joyway.in";
    private String fromName = "JOYWAY";
    private String replyTo = "hello@joyway.in";

    private String generalFrom = "hello@joyway.in";
    private String generalFromName = "JOYWAY";
    private String generalReplyTo = "hello@joyway.in";

    private String bookingsFrom = "bookings@joyway.in";
    private String bookingsFromName = "JOYWAY Bookings";
    private String bookingsReplyTo = "bookings@joyway.in";

    private String supportFrom = "support@joyway.in";
    private String supportFromName = "JOYWAY Support";
    private String supportReplyTo = "support@joyway.in";

    private String noreplyFrom = "noreply@joyway.in";
    private String noreplyFromName = "JOYWAY";
    private String noreplyReplyTo = "noreply@joyway.in";

    private String adminNotificationEmail = "hello@joyway.in";
    private String frontendBaseUrl = "http://localhost:3000";
    private boolean enabled = true;
    private boolean previewEnabled = false;
    private int maxRetryAttempts = 3;
    /** Base delay between SMTP retries. Each subsequent attempt doubles this. */
    private long retryDelayMs = 300_000;
}
