package in.joyway.config;

import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * PUBLIC_API_BASE_URL is baked into persisted image URLs at upload time, so an unset value
 * silently corrupts stored rows instead of failing the request. Production must refuse to start.
 */
@ExtendWith(MockitoExtension.class)
class ProductionSecurityValidatorBaseUrlTest {

    private static final String PROPERTY = "joyway.public-api-base-url";

    @Mock
    private Environment environment;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private CorsProperties corsProperties;

    @Mock
    private AdminBootstrapProperties adminBootstrapProperties;

    @Mock
    private RazorpayProperties razorpayProperties;

    private ProductionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductionSecurityValidator(
                environment,
                jwtProperties,
                corsProperties,
                adminBootstrapProperties,
                razorpayProperties,
                new StorageProperties(),
                new RateLimitProperties(),
                new MailProperties()
        );
    }

    @Test
    void acceptsHttpsProductionUrl() {
        when(environment.getProperty(PROPERTY)).thenReturn("https://api.joyway.in");

        assertDoesNotThrow(this::invokeValidation);
    }

    @Test
    void rejectsLocalhostFallback() {
        when(environment.getProperty(PROPERTY)).thenReturn("http://localhost:8080");

        assertThrows(IllegalStateException.class, this::invokeValidation);
    }

    @Test
    void rejectsLoopbackAddress() {
        when(environment.getProperty(PROPERTY)).thenReturn("http://127.0.0.1:8080");

        assertThrows(IllegalStateException.class, this::invokeValidation);
    }

    @Test
    void rejectsPlainHttp() {
        when(environment.getProperty(PROPERTY)).thenReturn("http://api.joyway.in");

        assertThrows(IllegalStateException.class, this::invokeValidation);
    }

    @Test
    void rejectsBlankValue() {
        when(environment.getProperty(PROPERTY)).thenReturn("");

        assertThrows(IllegalStateException.class, this::invokeValidation);
    }

    private void invokeValidation() {
        try {
            Method method = ProductionSecurityValidator.class.getDeclaredMethod("validatePublicApiBaseUrl");
            method.setAccessible(true);
            method.invoke(validator);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
