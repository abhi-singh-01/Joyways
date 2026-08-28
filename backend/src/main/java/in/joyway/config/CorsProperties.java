package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.cors")
@Getter
@Setter
public class CorsProperties {
    private String allowedOrigins = "http://localhost:3000";
}
