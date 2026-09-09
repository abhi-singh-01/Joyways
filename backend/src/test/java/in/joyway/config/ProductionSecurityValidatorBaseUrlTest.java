package in.joyway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "http://api.joyway.in",
            ""
    })
    void rejectsInvalidPublicApiBaseUrl(String value) {
        when(environment.getProperty(PROPERTY)).thenReturn(value);

        assertThrows(IllegalStateException.class, this::invokeValidation);
    }

    @Test
    void rejectsHttpCorsOrigin() {
        when(corsProperties.getAllowedOrigins()).thenReturn("http://joyway.in");
        assertThrows(IllegalStateException.class, this::invokeCorsValidation);
    }

    @Test
    void rejectsHttpFrontendBaseUrl() {
        MailProperties mail = new MailProperties();
        mail.setFrontendBaseUrl("http://joyway.in");
        validator = new ProductionSecurityValidator(
                environment,
                jwtProperties,
                corsProperties,
                adminBootstrapProperties,
                razorpayProperties,
                new StorageProperties(),
                new RateLimitProperties(),
                mail
        );
        assertThrows(IllegalStateException.class, this::invokeFrontendValidation);
    }

    private void invokeCorsValidation() {
        invokeNamed("validateCors");
    }

    private void invokeFrontendValidation() {
        invokeNamed("validateFrontendBaseUrl");
    }

    private void invokeNamed(String name) {
        try {
            Method method = ProductionSecurityValidator.class.getDeclaredMethod(name);
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
