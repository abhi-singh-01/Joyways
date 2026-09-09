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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapValidationTest {

    @Mock
    private Environment environment;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private CorsProperties corsProperties;

    @Mock
    private RazorpayProperties razorpayProperties;

    private AdminBootstrapProperties adminBootstrapProperties;
    private MailProperties mailProperties;
    private ProductionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        adminBootstrapProperties = new AdminBootstrapProperties();
        mailProperties = new MailProperties();
        validator = new ProductionSecurityValidator(
                environment,
                jwtProperties,
                corsProperties,
                adminBootstrapProperties,
                razorpayProperties,
                new StorageProperties(),
                new RateLimitProperties(),
                mailProperties
        );
    }

    @Test
    void validateAdminBootstrap_passesWhenDisabled() {
        adminBootstrapProperties.setEnabled(false);

        assertDoesNotThrow(this::invokeBootstrapValidation);
    }

    @Test
    void validateAdminBootstrap_rejectsEnabledInProduction() {
        adminBootstrapProperties.setEnabled(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                this::invokeBootstrapValidation
        );
        assertTrue(ex.getMessage().contains("ADMIN_BOOTSTRAP_ENABLED must be false"));
    }

    @Test
    void validateAdminBootstrap_rejectsPasswordWhenDisabledInProduction() {
        adminBootstrapProperties.setEnabled(false);
        adminBootstrapProperties.setPassword("leftover-bootstrap-secret");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                this::invokeBootstrapValidation
        );
        assertTrue(ex.getMessage().contains("ADMIN_BOOTSTRAP_PASSWORD must not be set"));
    }

    private void invokeBootstrapValidation() {
        try {
            Method method = ProductionSecurityValidator.class.getDeclaredMethod("validateAdminBootstrap");
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
