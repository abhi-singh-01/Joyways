package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expirationMs = 3600000;
    private boolean cookieSecure = false;
}
