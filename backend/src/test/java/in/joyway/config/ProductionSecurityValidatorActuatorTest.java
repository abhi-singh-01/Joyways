package in.joyway.config;

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

@ExtendWith(MockitoExtension.class)
class ProductionSecurityValidatorActuatorTest {

    private static final String EXPOSURE_PROPERTY = "management.endpoints.web.exposure.include";

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
    void acceptsEmptyActuatorExposure() {
        when(environment.getProperty(EXPOSURE_PROPERTY, "")).thenReturn("");

        assertDoesNotThrow(this::invokeActuatorValidation);
    }

    @Test
    void rejectsWildcardActuatorExposure() {
        when(environment.getProperty(EXPOSURE_PROPERTY, "")).thenReturn("*");

        assertThrows(IllegalStateException.class, this::invokeActuatorValidation);
    }

    @Test
    void rejectsForbiddenActuatorEndpointExposure() {
        when(environment.getProperty(EXPOSURE_PROPERTY, "")).thenReturn("health,env");

        assertThrows(IllegalStateException.class, this::invokeActuatorValidation);
    }

    private void invokeActuatorValidation() {
        try {
            Method method = ProductionSecurityValidator.class.getDeclaredMethod("validateActuatorExposure");
            method.setAccessible(true);
            method.invoke(validator);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new RuntimeException(ex.getCause());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
