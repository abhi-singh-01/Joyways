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

@ExtendWith(MockitoExtension.class)
class ProductionSecurityValidatorStorageTest {

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

    private StorageProperties storageProperties;
    private ProductionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        validator = new ProductionSecurityValidator(
                environment,
                jwtProperties,
                corsProperties,
                adminBootstrapProperties,
                razorpayProperties,
                storageProperties,
                new RateLimitProperties(),
                new MailProperties()
        );
    }

    @Test
    void validateStorageConfiguration_acceptsManagedIdentityAzureSetup() {
        storageProperties.setProvider("azure");
        storageProperties.setEndpoint("https://joywayprod.blob.core.windows.net");
        storageProperties.setContainer("joyway-documents");
        storageProperties.setUseManagedIdentity(true);

        assertDoesNotThrow(this::invokeStorageValidation);
    }

    @Test
    void validateStorageConfiguration_rejectsLocalProvider() {
        storageProperties.setProvider("local");
        storageProperties.setLocalPath("./storage");

        assertThrows(IllegalStateException.class, this::invokeStorageValidation);
    }

    @Test
    void validateStorageConfiguration_requiresSharedKeyWhenManagedIdentityDisabled() {
        storageProperties.setProvider("azure");
        storageProperties.setEndpoint("https://joywayprod.blob.core.windows.net");
        storageProperties.setContainer("joyway-documents");
        storageProperties.setUseManagedIdentity(false);

        assertThrows(IllegalStateException.class, this::invokeStorageValidation);
    }

    @Test
    void validateStorageConfiguration_requiresSecretKeyWhenAccessKeyPresent() {
        storageProperties.setProvider("azure");
        storageProperties.setEndpoint("https://joywayprod.blob.core.windows.net");
        storageProperties.setContainer("joyway-documents");
        storageProperties.setAccessKey("account-key");
        storageProperties.setUseManagedIdentity(false);

        assertThrows(IllegalStateException.class, this::invokeStorageValidation);
    }

    @Test
    void validateStorageConfiguration_rejectsLocalhostEndpoint() {
        storageProperties.setProvider("azure");
        storageProperties.setEndpoint("http://127.0.0.1:10000/devstoreaccount1");
        storageProperties.setContainer("joyway-documents");
        storageProperties.setAccessKey("devstoreaccount1");
        storageProperties.setSecretKey("placeholder-key");
        storageProperties.setUseManagedIdentity(false);

        assertThrows(IllegalStateException.class, this::invokeStorageValidation);
    }

    private void invokeStorageValidation() {
        try {
            Method method = ProductionSecurityValidator.class.getDeclaredMethod("validateStorageConfiguration");
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
