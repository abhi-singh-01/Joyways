package in.joyway.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fail-fast validation for production security configuration.
 */
@Component
@Profile({"prod", "production"})
@RequiredArgsConstructor
@Slf4j
public class ProductionSecurityValidator {

    private static final List<String> INSECURE_JWT_PATTERNS = List.of(
            "change-me",
            "changeme",
            "your-secret",
            "secret",
            "test-secret",
            "jwt-secret",
            "placeholder"
    );

    private static final Pattern LOCALHOST_PATTERN = Pattern.compile(
            "localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0",
            Pattern.CASE_INSENSITIVE
    );

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final AdminBootstrapProperties adminBootstrapProperties;
    private final RazorpayProperties razorpayProperties;
    private final StorageProperties storageProperties;

    @PostConstruct
    void validateProductionSecurity() {
        log.info("Validating production security configuration...");

        validateJwtSecret();
        validateJwtCookieSecure();
        validateCors();
        validateAdminBootstrap();
        validateDatabaseUrl();
        validatePublicApiBaseUrl();
        validateRazorpaySecrets();
        validateStorageConfiguration();

        log.info("Production security validation passed.");
    }

    private void validateJwtSecret() {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET must be set in production.");
        }
        String lower = secret.toLowerCase(Locale.ROOT);
        for (String pattern : INSECURE_JWT_PATTERNS) {
            if (lower.contains(pattern)) {
                throw new IllegalStateException(
                        "JWT_SECRET appears to use an insecure placeholder value. Set a strong production secret.");
            }
        }
    }

    private void validateJwtCookieSecure() {
        if (!jwtProperties.isCookieSecure()) {
            throw new IllegalStateException(
                    "JWT_COOKIE_SECURE must be true in production (joyway.jwt.cookie-secure).");
        }
    }

    private void validateCors() {
        String origins = corsProperties.getAllowedOrigins();
        if (!StringUtils.hasText(origins)) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be set in production.");
        }
        if ("*".equals(origins.trim())) {
            throw new IllegalStateException("CORS must not use wildcard (*) in production.");
        }
        Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(origin -> {
                    if (LOCALHOST_PATTERN.matcher(origin).find()) {
                        throw new IllegalStateException(
                                "CORS_ALLOWED_ORIGINS must not include localhost in production: " + origin);
                    }
                    if (!origin.startsWith("https://")) {
                        throw new IllegalStateException(
                                "Production CORS origins should use HTTPS: " + origin);
                    }
                });
    }

    private static final int MIN_BOOTSTRAP_PASSWORD_LENGTH = 12;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private void validateAdminBootstrap() {
        if (!adminBootstrapProperties.isEnabled()) {
            return;
        }

        log.warn("ADMIN_BOOTSTRAP_ENABLED=true in production. "
                + "This should only be used for initial admin setup. "
                + "Disable immediately after creating the first admin.");

        String email = adminBootstrapProperties.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_EMAIL must be set when bootstrap is enabled.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_EMAIL must be a valid email address.");
        }

        String password = adminBootstrapProperties.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD must be set when bootstrap is enabled.");
        }
        if (password.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD must be at least " + MIN_BOOTSTRAP_PASSWORD_LENGTH
                            + " characters for production security.");
        }
    }

    private void validateDatabaseUrl() {
        String databaseUrl = environment.getProperty("spring.datasource.url");
        if (!StringUtils.hasText(databaseUrl)) {
            throw new IllegalStateException("DATABASE_URL must be set in production.");
        }
        if (LOCALHOST_PATTERN.matcher(databaseUrl).find()) {
            throw new IllegalStateException("DATABASE_URL must not point to localhost in production.");
        }
    }

    /**
     * Uploaded file URLs are persisted into package_images.image_url with this value as the prefix,
     * so a wrong base URL corrupts stored data rather than just breaking the current request.
     */
    private void validatePublicApiBaseUrl() {
        String baseUrl = environment.getProperty("joyway.public-api-base-url");
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("PUBLIC_API_BASE_URL must be set in production.");
        }
        if (LOCALHOST_PATTERN.matcher(baseUrl).find()) {
            throw new IllegalStateException(
                    "PUBLIC_API_BASE_URL must not point to localhost in production: " + baseUrl);
        }
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "PUBLIC_API_BASE_URL must use HTTPS in production: " + baseUrl);
        }
    }

    private void validateRazorpaySecrets() {
        if (!StringUtils.hasText(razorpayProperties.getKeySecret())) {
            throw new IllegalStateException("RAZORPAY_KEY_SECRET must be set in production.");
        }
        if (!StringUtils.hasText(razorpayProperties.getWebhookSecret())) {
            throw new IllegalStateException(
                    "RAZORPAY_WEBHOOK_SECRET must be set in production for webhook verification.");
        }
    }

    private void validateStorageConfiguration() {
        if (!storageProperties.isAzureProvider()) {
            throw new IllegalStateException(
                    "STORAGE_PROVIDER must be 'azure' in production. Local filesystem storage is not permitted.");
        }
        if (!StringUtils.hasText(storageProperties.getEndpoint())) {
            throw new IllegalStateException("STORAGE_ENDPOINT must be set in production.");
        }
        if (LOCALHOST_PATTERN.matcher(storageProperties.getEndpoint()).find()) {
            throw new IllegalStateException("STORAGE_ENDPOINT must not point to localhost in production.");
        }
        if (!StringUtils.hasText(storageProperties.getContainerName())) {
            throw new IllegalStateException("STORAGE_CONTAINER (or STORAGE_BUCKET) must be set in production.");
        }
        if (!storageProperties.isUseManagedIdentity()) {
            if (!StringUtils.hasText(storageProperties.getAccessKey())
                    || !StringUtils.hasText(storageProperties.getSecretKey())) {
                throw new IllegalStateException(
                        "STORAGE_ACCESS_KEY and STORAGE_SECRET_KEY are required in production when "
                                + "STORAGE_USE_MANAGED_IDENTITY=false. Prefer Managed Identity.");
            }
        }
    }
}
