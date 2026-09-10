package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.mail")
@Getter
@Setter
public class MailProperties {

    private static final String DEFAULT_HELLO_EMAIL = "hello@joyway.in";
    private static final String DEFAULT_BRAND_NAME = "JOYWAY";

    /** Legacy fallback; mirrors general sender. */
    private String from = DEFAULT_HELLO_EMAIL;
    private String fromName = DEFAULT_BRAND_NAME;
    private String replyTo = DEFAULT_HELLO_EMAIL;

    private String generalFrom = DEFAULT_HELLO_EMAIL;
    private String generalFromName = DEFAULT_BRAND_NAME;
    private String generalReplyTo = DEFAULT_HELLO_EMAIL;

    private String bookingsFrom = "bookings@joyway.in";
    private String bookingsFromName = "JOYWAY Bookings";
    private String bookingsReplyTo = "bookings@joyway.in";

    private String supportFrom = "support@joyway.in";
    private String supportFromName = "JOYWAY Support";
    private String supportReplyTo = "support@joyway.in";

    private String noreplyFrom = "noreply@joyway.in";
    private String noreplyFromName = DEFAULT_BRAND_NAME;
    private String noreplyReplyTo = "noreply@joyway.in";

    private String adminNotificationEmail = DEFAULT_HELLO_EMAIL;
    private String frontendBaseUrl = "http://localhost:3000";
    /** E.164 without +. Used for customer WhatsApp confirmation links in email. */
    private String whatsappNumber = "917050971956";
    private boolean enabled = true;
    private boolean previewEnabled = false;
    private int maxRetryAttempts = 3;
    /** Base delay between SMTP retries. Each subsequent attempt doubles this. */
    private long retryDelayMs = 300_000;
}
