package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;

    /** Admin login attempts allowed per client per minute. */
    private int loginRequestsPerMinute = 10;

    /** Booking creation attempts allowed per client per minute. */
    private int bookingRequestsPerMinute = 20;

    /** Payment order/verify attempts allowed per client per minute. */
    private int paymentRequestsPerMinute = 30;
}
