package in.joyway.service;

import in.joyway.domain.entity.Role;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.RoleRepository;
import in.joyway.domain.repository.UserRepository;
import in.joyway.dto.admin.LoginRequest;
import in.joyway.security.JoywayUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAuthServiceIntegrationTest {

    private static final String ADMIN_EMAIL = "rockabhi0977@joyway.in";
    private static final String ADMIN_PASSWORD = "SecurePassword123!";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AdminAuthService adminAuthService;

    @BeforeEach
    void seedAdminUser() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .name("JOYWAY Admin")
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());
    }

    @Test
    void authenticationManagerUsesJoywayUserDetailsServiceAndBcrypt() {
        assertTrue(userDetailsService instanceof JoywayUserDetailsService);
        assertTrue(passwordEncoder.getClass().getSimpleName().contains("BCrypt"));

        assertDoesNotThrow(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(ADMIN_EMAIL, ADMIN_PASSWORD)
        ));
    }

    @Test
    void authenticationManager_authenticatesMixedCaseEmail() {
        assertDoesNotThrow(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("RockAbhi0977@Joyway.in", ADMIN_PASSWORD)
        ));
    }

    @Test
    void adminAuthService_login_succeedsForSeededAdmin() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        LoginRequest request = new LoginRequest();
        request.setEmail(ADMIN_EMAIL);
        request.setPassword(ADMIN_PASSWORD);

        assertDoesNotThrow(() -> adminAuthService.login(request, response));
    }

    @Test
    void adminAuthService_login_succeedsWithMixedCaseEmail() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        LoginRequest request = new LoginRequest();
        request.setEmail("RockAbhi0977@Joyway.in");
        request.setPassword(ADMIN_PASSWORD);

        assertDoesNotThrow(() -> adminAuthService.login(request, response));
    }

    @Test
    void adminAuthService_login_omitsCookieDomainWhenUnset() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        LoginRequest request = new LoginRequest();
        request.setEmail(ADMIN_EMAIL);
        request.setPassword(ADMIN_PASSWORD);

        adminAuthService.login(request, response);

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).addHeader(Mockito.eq(HttpHeaders.SET_COOKIE), value.capture());
        assertFalse(value.getValue().contains("Domain="), value.getValue());
    }

    @Test
    void adminAuthService_login_failsWithWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail(ADMIN_EMAIL);
        request.setPassword("wrong-password");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> adminAuthService.login(request, response)
        );
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    void adminAuthService_login_failsWhenAdminRoleMissing() {
        User user = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        user.setRoles(new HashSet<>());
        userRepository.saveAndFlush(user);

        LoginRequest request = new LoginRequest();
        request.setEmail(ADMIN_EMAIL);
        request.setPassword(ADMIN_PASSWORD);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> adminAuthService.login(request, response)
        );
        assertEquals("Invalid email or password.", ex.getMessage());
    }
}
