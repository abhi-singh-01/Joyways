package in.joyway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures application-prod.yml does not hard-code bootstrap disabled, which would
 * override ADMIN_BOOTSTRAP_ENABLED from the container environment.
 */
class AdminBootstrapProdProfilePropertiesTest {

    @Test
    void prodProfileDefaultsBootstrapDisabledWhenEnvUnset() throws Exception {
        AdminBootstrapProperties properties = bindProdBootstrapProperties(Map.of());

        assertFalse(properties.isEnabled());
    }

    @Test
    void prodProfileEnablesBootstrapWhenEnvVarTrue() throws Exception {
        AdminBootstrapProperties properties = bindProdBootstrapProperties(Map.of(
                "ADMIN_BOOTSTRAP_ENABLED", "true"
        ));

        assertTrue(properties.isEnabled());
    }

    private AdminBootstrapProperties bindProdBootstrapProperties(Map<String, Object> envVars) throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

        loader.load("application", new ClassPathResource("application.yml"))
                .forEach(source -> environment.getPropertySources().addLast(source));
        loader.load("application-prod", new ClassPathResource("application-prod.yml"))
                .forEach(source -> environment.getPropertySources().addFirst(source));

        if (!envVars.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("testEnv", envVars));
        }

        return Binder.get(environment)
                .bind("joyway.admin.bootstrap", AdminBootstrapProperties.class)
                .orElseThrow(() -> new IllegalStateException("Failed to bind joyway.admin.bootstrap"));
    }
}
