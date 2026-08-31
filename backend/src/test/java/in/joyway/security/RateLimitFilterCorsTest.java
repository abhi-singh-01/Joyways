package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitFilterCorsTest {

    private static final String ALLOWED_ORIGIN = "https://joyway.in";
    private static final String LOGIN_PATH = "/api/admin/auth/login";

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setLoginRequestsPerMinute(1);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(ALLOWED_ORIGIN));
        configuration.setAllowedMethods(List.of("POST"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        filter = new RateLimitFilter(properties, new ObjectMapper(), source);
    }

    private MockHttpServletResponse loginAttempt(String origin, String clientIp) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN_PATH);
        request.setRequestURI(LOGIN_PATH);
        request.setRemoteAddr(clientIp);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void rateLimitedResponse_includesCorsHeadersForAllowedOrigin() throws Exception {
        loginAttempt(ALLOWED_ORIGIN, "10.0.0.1");
        MockHttpServletResponse throttled = loginAttempt(ALLOWED_ORIGIN, "10.0.0.1");

        assertEquals(429, throttled.getStatus());
        assertEquals(ALLOWED_ORIGIN, throttled.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", throttled.getHeader("Access-Control-Allow-Credentials"));
        assertEquals("60", throttled.getHeader("Retry-After"));
    }

    @Test
    void rateLimitedResponse_omitsCorsHeadersForDisallowedOrigin() throws Exception {
        loginAttempt("https://evil.example", "10.0.0.2");
        MockHttpServletResponse throttled = loginAttempt("https://evil.example", "10.0.0.2");

        assertEquals(429, throttled.getStatus());
        assertNull(throttled.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void rateLimitedResponse_omitsCorsHeadersWhenNoOriginHeader() throws Exception {
        loginAttempt(null, "10.0.0.3");
        MockHttpServletResponse throttled = loginAttempt(null, "10.0.0.3");

        assertEquals(429, throttled.getStatus());
        assertNull(throttled.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void requestWithinLimit_passesThroughWithoutThrottling() throws Exception {
        MockHttpServletResponse response = loginAttempt(ALLOWED_ORIGIN, "10.0.0.4");

        assertEquals(200, response.getStatus());
    }
}
