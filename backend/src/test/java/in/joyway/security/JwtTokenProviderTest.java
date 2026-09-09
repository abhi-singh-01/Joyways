package in.joyway.security;

import in.joyway.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("local-test-jwt-secret-key-32chars!");
        properties.setExpirationMs(3_600_000);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generateToken_roundTripsEmailAndRoles() {
        String token = jwtTokenProvider.generateToken("admin@joyway.in", List.of("ROLE_ADMIN"));

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("admin@joyway.in");
        assertThat(jwtTokenProvider.getRolesFromToken(token)).containsExactly("ROLE_ADMIN");
    }

    @Test
    void validateToken_rejectsTamperedToken() {
        String token = jwtTokenProvider.generateToken("admin@joyway.in", List.of("ROLE_ADMIN"));

        assertThat(jwtTokenProvider.validateToken(token + "x")).isFalse();
        assertThat(jwtTokenProvider.validateToken("not-a-jwt")).isFalse();
    }
}
