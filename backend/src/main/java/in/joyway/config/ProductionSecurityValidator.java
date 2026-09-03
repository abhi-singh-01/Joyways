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

    private static final List<String> FORBIDDEN_ACTUATOR_ENDPOINTS = List.of(
            "env",
            "beans",
            "configprops",
            "mappings",
            "threaddump",
            "heapdump",
            "loggers"
    );

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final AdminBootstrapProperties adminBootstrapProperties;
    private final RazorpayProperties razorpayProperties;
    private final StorageProperties storageProperties;
    private final RateLimitProperties rateLimitProperties;
    private final MailProperties mailProperties;

    @PostConstruct
    void validateProductionSecurity() {
        log.info("Validating production security configuration...");

        validateJwtSecret();
        validateJwtCookieSecure();
        validateCors();
        validateAdminBootstrap();
        validateDatabaseUrl();
        validatePublicApiBaseUrl();
        validateFrontendBaseUrl();
        validateRazorpaySecrets();
        validateStorageConfiguration();
        validateRateLimitsEnabled();
        validateRateLimitRedisConfiguration();
        validateJwtCookieDomain();
        validateMailPreviewDisabled();
        validateActuatorExposure();

        log.info("Production security validation passed.");
    }

    private void validateActuatorExposure() {
        String exposureInclude = environment.getProperty(
                "management.endpoints.web.exposure.include", "");
        if (!StringUtils.hasText(exposureInclude)) {
            return;
        }
        String trimmed = exposureInclude.trim();
        if ("*".equals(trimmed)) {
            throw new IllegalStateException(
                    "management.endpoints.web.exposure.include must not use wildcard (*) in production.");
        }
        Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(endpoint -> {
                    String normalized = endpoint.toLowerCase(Locale.ROOT);
                    if (FORBIDDEN_ACTUATOR_ENDPOINTS.contains(normalized)) {
                        throw new IllegalStateException(
                                "Actuator endpoint must not be exposed in production: " + endpoint);
                    }
                });
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

    private void validateAdminBootstrap() {
        if (adminBootstrapProperties.isEnabled()) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_ENABLED must be false in production after the first admin is created.");
        }
        if (StringUtils.hasText(adminBootstrapProperties.getPassword())) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD must not be set in production when bootstrap is disabled. "
                            + "Remove the bootstrap password from the container environment/secrets.");
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
     * PUBLIC_API_BASE_URL is used to build public package image URLs at read time from Azure
     * blob storage keys — URLs are not persisted in the database.
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
        if (!StringUtils.hasText(razorpayProperties.getKeyId())) {
            throw new IllegalStateException("RAZORPAY_KEY_ID must be set in production.");
        }
        if (!StringUtils.hasText(razorpayProperties.getWebhookSecret())) {
            throw new IllegalStateException(
                    "RAZORPAY_WEBHOOK_SECRET must be set in production for webhook verification.");
        }
        if (razorpayProperties.getWebhookSecret().length() < 32) {
            throw new IllegalStateException(
                    "RAZORPAY_WEBHOOK_SECRET must be at least 32 characters in production.");
        }
        String keyId = razorpayProperties.getKeyId();
        if (StringUtils.hasText(keyId) && keyId.toLowerCase(Locale.ROOT).startsWith("rzp_test_")) {
            throw new IllegalStateException(
                    "RAZORPAY_KEY_ID must be a live key (rzp_live_...) in production.");
        }
    }

    private void validateFrontendBaseUrl() {
        String baseUrl = mailProperties.getFrontendBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("FRONTEND_BASE_URL must be set in production.");
        }
        if (LOCALHOST_PATTERN.matcher(baseUrl).find()) {
            throw new IllegalStateException(
                    "FRONTEND_BASE_URL must not point to localhost in production: " + baseUrl);
        }
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "FRONTEND_BASE_URL must use HTTPS in production: " + baseUrl);
        }
    }

    private void validateMailPreviewDisabled() {
        if (mailProperties.isPreviewEnabled()) {
            throw new IllegalStateException("MAIL_PREVIEW_ENABLED must be false in production.");
        }
    }

    private void validateRateLimitsEnabled() {
        if (!rateLimitProperties.isEnabled()) {
            throw new IllegalStateException("RATE_LIMIT_ENABLED must be true in production.");
        }
    }

    private void validateRateLimitRedisConfiguration() {
        if (!rateLimitProperties.isEnabled() || !rateLimitProperties.isRedisBackend()) {
            return;
        }
        if (!StringUtils.hasText(rateLimitProperties.getRedisHost())) {
            throw new IllegalStateException(
                    "REDIS_HOST must be set in production when RATE_LIMIT_BACKEND=redis.");
        }
        if (rateLimitProperties.getRedisPort() <= 0) {
            throw new IllegalStateException(
                    "REDIS_PORT must be set in production when RATE_LIMIT_BACKEND=redis.");
        }
        if (!StringUtils.hasText(rateLimitProperties.getRedisPassword())) {
            throw new IllegalStateException(
                    "REDIS_PASSWORD must be set in production when RATE_LIMIT_BACKEND=redis.");
        }
    }

    private void validateJwtCookieDomain() {
        String domain = jwtProperties.getCookieDomain();
        if (!StringUtils.hasText(domain)) {
            throw new IllegalStateException(
                    "JWT_COOKIE_DOMAIN must be set in production (e.g. joyway.in) so the admin cookie is shared between joyway.in and api.joyway.in.");
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
