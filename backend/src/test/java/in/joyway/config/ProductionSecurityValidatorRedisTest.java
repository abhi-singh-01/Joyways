package in.joyway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionSecurityValidatorRedisTest {

    private RateLimitProperties rateLimitProperties;
    private ProductionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);

        validator = new ProductionSecurityValidator(
                new org.springframework.mock.env.MockEnvironment(),
                new JwtProperties(),
                new CorsProperties(),
                new AdminBootstrapProperties(),
                new RazorpayProperties(),
                new StorageProperties(),
                rateLimitProperties,
                new MailProperties()
        );
    }

    @Test
    void memoryBackendDoesNotRequireRedisConfiguration() {
        rateLimitProperties.setBackend("memory");
        assertDoesNotThrow(this::invokeValidateRateLimitRedisConfiguration);
    }

    @Test
    void redisBackendRequiresHostPasswordAndPort() {
        rateLimitProperties.setBackend("redis");
        rateLimitProperties.setRedisHost("");
        rateLimitProperties.setRedisPassword("");
        rateLimitProperties.setRedisPort(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                this::invokeValidateRateLimitRedisConfiguration);

        assertTrue(ex.getMessage().contains("REDIS_HOST"));
    }

    @Test
    void redisBackendPassesWithRequiredSettings() {
        rateLimitProperties.setBackend("redis");
        rateLimitProperties.setRedisHost("joyway-redis.example.com");
        rateLimitProperties.setRedisPassword("secret");
        rateLimitProperties.setRedisPort(10000);

        assertDoesNotThrow(this::invokeValidateRateLimitRedisConfiguration);
    }

    private void invokeValidateRateLimitRedisConfiguration() {
        try {
            var method = ProductionSecurityValidator.class.getDeclaredMethod("validateRateLimitRedisConfiguration");
            method.setAccessible(true);
            method.invoke(validator);
        } catch (ReflectiveOperationException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(ex);
        }
    }
}
