package in.joyway.config;

import in.joyway.domain.entity.Role;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.RoleRepository;
import in.joyway.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapProperties bootstrapProperties;
    private AdminBootstrapConfig config;

    @BeforeEach
    void setUp() {
        bootstrapProperties = new AdminBootstrapProperties();
        config = new AdminBootstrapConfig(
                bootstrapProperties,
                userRepository,
                roleRepository,
                passwordEncoder
        );
    }

    @Test
    void bootstrapAdmin_doesNothingWhenDisabled() throws Exception {
        bootstrapProperties.setEnabled(false);

        CommandLineRunner runner = config.bootstrapAdmin();
        runner.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void bootstrapAdmin_createsAdminWithValidConfiguration() throws Exception {
        bootstrapProperties.setEnabled(true);
        bootstrapProperties.setEmail("admin@joyway.in");
        bootstrapProperties.setPassword("SecurePassword123!");
        bootstrapProperties.setName("Test Admin");

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ROLE_ADMIN");

        when(userRepository.existsByEmail("admin@joyway.in")).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encoded-hash");

        CommandLineRunner runner = config.bootstrapAdmin();
        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("admin@joyway.in", savedUser.getEmail());
        assertEquals("Test Admin", savedUser.getName());
        assertEquals("encoded-hash", savedUser.getPasswordHash());
        assertTrue(savedUser.getEnabled());
        assertTrue(savedUser.getRoles().contains(adminRole));
    }

    @Test
    void bootstrapAdmin_skipsCreationWhenAdminExists() throws Exception {
        bootstrapProperties.setEnabled(true);
        bootstrapProperties.setEmail("admin@joyway.in");
        bootstrapProperties.setPassword("SecurePassword123!");

        when(userRepository.existsByEmail("admin@joyway.in")).thenReturn(true);

        CommandLineRunner runner = config.bootstrapAdmin();
        runner.run();

        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void bootstrapAdmin_throwsWhenPasswordMissing() {
        bootstrapProperties.setEnabled(true);
        bootstrapProperties.setEmail("admin@joyway.in");
        bootstrapProperties.setPassword("");

        when(userRepository.existsByEmail("admin@joyway.in")).thenReturn(false);

        CommandLineRunner runner = config.bootstrapAdmin();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> runner.run()
        );
        assertTrue(ex.getMessage().contains("ADMIN_BOOTSTRAP_PASSWORD is required"));
    }

    @Test
    void bootstrapAdmin_throwsWhenRoleAdminNotFound() {
        bootstrapProperties.setEnabled(true);
        bootstrapProperties.setEmail("admin@joyway.in");
        bootstrapProperties.setPassword("SecurePassword123!");

        when(userRepository.existsByEmail("admin@joyway.in")).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

        CommandLineRunner runner = config.bootstrapAdmin();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> runner.run()
        );
        assertTrue(ex.getMessage().contains("ROLE_ADMIN not found"));
    }

    @Test
    void bootstrapAdmin_usesDefaultNameWhenNotProvided() throws Exception {
        bootstrapProperties.setEnabled(true);
        bootstrapProperties.setEmail("admin@joyway.in");
        bootstrapProperties.setPassword("SecurePassword123!");

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ROLE_ADMIN");

        when(userRepository.existsByEmail("admin@joyway.in")).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        CommandLineRunner runner = config.bootstrapAdmin();
        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals("JOYWAY Admin", userCaptor.getValue().getName());
    }
}
