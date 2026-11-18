package in.joyway.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts Render / Neon / Supabase {@code postgres://} URLs into Spring JDBC settings.
 * Spring Boot does not accept {@code postgresql://user:pass@host/db} as {@code spring.datasource.url}.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "joywayDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("spring.datasource.url")
        );
        if (!StringUtils.hasText(raw) || raw.startsWith("jdbc:")) {
            return;
        }
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            return;
        }

        ParsedJdbc parsed = parsePostgresUrl(raw);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", parsed.jdbcUrl());
        if (!StringUtils.hasText(environment.getProperty("DATABASE_USERNAME"))
                && !StringUtils.hasText(environment.getProperty("spring.datasource.username"))
                && StringUtils.hasText(parsed.username())) {
            properties.put("spring.datasource.username", parsed.username());
        }
        if (!StringUtils.hasText(environment.getProperty("DATABASE_PASSWORD"))
                && !StringUtils.hasText(environment.getProperty("spring.datasource.password"))
                && parsed.password() != null) {
            properties.put("spring.datasource.password", parsed.password());
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    static ParsedJdbc parsePostgresUrl(String raw) {
        String normalized = raw.replaceFirst("^postgres://", "postgresql://");
        URI uri;
        try {
            uri = new URI(normalized);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("DATABASE_URL is not a valid PostgreSQL URL.", ex);
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new IllegalStateException("DATABASE_URL is missing a host.");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getPath() == null ? "" : uri.getPath();
        if (database.startsWith("/")) {
            database = database.substring(1);
        }
        if (!StringUtils.hasText(database)) {
            throw new IllegalStateException("DATABASE_URL is missing a database name.");
        }
        String query = uri.getRawQuery();
        if (!StringUtils.hasText(query)) {
            query = "sslmode=require";
        } else if (!query.toLowerCase().contains("sslmode=")) {
            query = query + "&sslmode=require";
        }
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + database + "?" + query;

        String username = "";
        String password = "";
        String userInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(userInfo)) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = decode(userInfo.substring(0, colon));
                password = decode(userInfo.substring(colon + 1));
            } else {
                username = decode(userInfo);
            }
        }
        return new ParsedJdbc(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        return b;
    }

    record ParsedJdbc(String jdbcUrl, String username, String password) {
    }
}
