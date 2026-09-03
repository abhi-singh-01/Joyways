package in.joyway.service;

import in.joyway.domain.entity.Role;
import in.joyway.domain.entity.User;
import in.joyway.domain.repository.RoleRepository;
import in.joyway.domain.repository.UserRepository;
import in.joyway.dto.admin.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The admin UI (joyway.in) and the API (api.joyway.in) are different hosts of the same site,
 * so the auth cookie must carry an explicit Domain or the browser scopes it to the API host only.
 */
@SpringBootTest(properties = "joyway.jwt.cookie-domain=.joyway.in")
@ActiveProfiles("test")
@Transactional
class AdminAuthServiceCookieDomainTest {

    private static final String ADMIN_EMAIL = "cookie-admin@joyway.in";
    private static final String ADMIN_PASSWORD = "SecurePassword123!";

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void login_setsCookieWithConfiguredDomain() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        LoginRequest request = new LoginRequest();
        request.setEmail(ADMIN_EMAIL);
        request.setPassword(ADMIN_PASSWORD);

        adminAuthService.login(request, response);

        String setCookie = captureSetCookie(response);
        assertTrue(setCookie.contains("Domain=joyway.in"), setCookie);
        assertTrue(setCookie.contains("Path=/"), setCookie);
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
    }

    @Test
    void logout_clearsCookieWithSameDomain() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        adminAuthService.logout(response);

        String setCookie = captureSetCookie(response);
        assertTrue(setCookie.contains("Domain=joyway.in"), setCookie);
        assertTrue(setCookie.contains("Max-Age=0"), setCookie);
    }

    private String captureSetCookie(HttpServletResponse response) {
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).addHeader(Mockito.eq(HttpHeaders.SET_COOKIE), value.capture());
        return value.getValue();
    }
}
