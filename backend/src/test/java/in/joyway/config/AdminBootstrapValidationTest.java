package in.joyway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

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
    private StorageProperties storageProperties;
    private ProductionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        adminBootstrapProperties = new AdminBootstrapProperties();
        storageProperties = new StorageProperties();
        validator = new ProductionSecurityValidator(
                environment,
                jwtProperties,
                corsProperties,
                adminBootstrapProperties,
                razorpayProperties,
                storageProperties
        );
    }

    @Test
    void validateAdminBootstrap_passesWhenDisabled() {
        adminBootstrapProperties.setEnabled(false);

        assertDoesNotThrow(this::invokeBootstrapValidation);
    }

    @Test
    void validateAdminBootstrap_passesWithValidConfiguration() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("admin@joyway.in");
        adminBootstrapProperties.setPassword("SecurePassword123!");
        adminBootstrapProperties.setName("Test Admin");

        assertDoesNotThrow(this::invokeBootstrapValidation);
    }

    @Test
    void validateAdminBootstrap_rejectsEmptyEmail() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("");
        adminBootstrapProperties.setPassword("SecurePassword123!");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                this::invokeBootstrapValidation
        );
        assertTrue(ex.getMessage().contains("ADMIN_BOOTSTRAP_EMAIL must be set"));
    }

    @Test
    void validateAdminBootstrap_rejectsInvalidEmailFormat() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("not-an-email");
        adminBootstrapProperties.setPassword("SecurePassword123!");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                this::invokeBootstrapValidation
        );
        assertTrue(ex.getMessage().contains("valid email address"));
    }

    @Test
    void validateAdminBootstrap_rejectsEmptyPassword() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("admin@joyway.in");
        adminBootstrapProperties.setPassword("");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                this::invokeBootstrapValidation
        );
        assertTrue(ex.getMessage().contains("ADMIN_BOOTSTRAP_PASSWORD must be set"));
    }

    @Test
    void validateAdminBootstrap_rejectsWeakPassword() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("admin@joyway.in");
        adminBootstrapProperties.setPassword("short");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                this::invokeBootstrapValidation
        );
        assertTrue(ex.getMessage().contains("at least 12 characters"));
    }

    @Test
    void validateAdminBootstrap_acceptsExactlyMinimumPasswordLength() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("admin@joyway.in");
        adminBootstrapProperties.setPassword("123456789012");

        assertDoesNotThrow(this::invokeBootstrapValidation);
    }

    @Test
    void validateAdminBootstrap_acceptsEmailWithSubdomain() {
        adminBootstrapProperties.setEnabled(true);
        adminBootstrapProperties.setEmail("admin@mail.joyway.in");
        adminBootstrapProperties.setPassword("SecurePassword123!");

        assertDoesNotThrow(this::invokeBootstrapValidation);
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
