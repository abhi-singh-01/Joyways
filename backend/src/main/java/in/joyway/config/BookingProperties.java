package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "joyway.booking")
@Getter
@Setter
public class BookingProperties {

    /** Unpaid bookings older than this are expired and seats released. */
    private int paymentWindowMinutes = 30;
}
