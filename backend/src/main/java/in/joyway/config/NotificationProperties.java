package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.notifications")
@Getter
@Setter
public class NotificationProperties {
    private boolean tripReminderEnabled = true;
    private int tripReminderDaysBefore = 7;
    private boolean tripReminderDayBeforeEnabled = true;
}
