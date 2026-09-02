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
    private static final String LOGIN_PATH = RateLimitFilter.LOGIN_PATH;

    private RateLimitFilter filter;
    private InMemoryRateLimitBackend backend;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setLoginRequestsPerMinute(1);

        backend = new InMemoryRateLimitBackend(java.time.Duration.ofMinutes(1));

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(ALLOWED_ORIGIN));
        configuration.setAllowedMethods(List.of("POST", "OPTIONS"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        filter = new RateLimitFilter(
                properties,
                new ObjectMapper(),
                source,
                backend,
                new ClientAddressResolver()
        );
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

    @Test
    void rateLimit_usesRightMostForwardedForHop() throws Exception {
        MockHttpServletRequest first = new MockHttpServletRequest("POST", LOGIN_PATH);
        first.setRequestURI(LOGIN_PATH);
        first.setRemoteAddr("10.0.0.8");
        first.addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.9");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest spoofed = new MockHttpServletRequest("POST", LOGIN_PATH);
        spoofed.setRequestURI(LOGIN_PATH);
        spoofed.setRemoteAddr("10.0.0.8");
        spoofed.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.9");
        MockHttpServletResponse throttled = new MockHttpServletResponse();
        filter.doFilter(spoofed, throttled, new MockFilterChain());

        assertEquals(429, throttled.getStatus());
    }

    @Test
    void optionsPreflightBypassesRateLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/bookings/lookup");
            options.setRequestURI("/api/bookings/lookup");
            options.setRemoteAddr("10.0.0.50");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(options, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }
}
