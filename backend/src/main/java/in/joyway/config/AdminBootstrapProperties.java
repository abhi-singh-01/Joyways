package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joyway.admin.bootstrap")
@Getter
@Setter
public class AdminBootstrapProperties {
    private boolean enabled = false;
    private String email = "admin@joyway.in";
    private String password = "";
    private String name = "JOYWAY Admin";
}
