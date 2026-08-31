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

    /**
     * Cookie Domain attribute. Must be set when the API and the admin UI are served from
     * different hosts of the same site (api.joyway.in vs joyway.in), otherwise the cookie is
     * host-only and the UI cannot see it. Leave blank for single-host setups such as localhost.
     */
    private String cookieDomain;
}
