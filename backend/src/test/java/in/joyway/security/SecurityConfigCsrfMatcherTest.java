package in.joyway.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigCsrfMatcherTest {

    private final RequestMatcher matcher = SecurityConfig.adminStateChangingCsrfMatcher();

    @Test
    void requiresCsrfForAdminMutations() {
        assertTrue(matcher.matches(request("POST", "/api/admin/packages")));
        assertTrue(matcher.matches(request("PUT", "/api/admin/bookings/1")));
        assertTrue(matcher.matches(request("DELETE", "/api/admin/trips/1")));
    }

    @Test
    void skipsCsrfForAdminLoginAndGets() {
        assertFalse(matcher.matches(request("POST", "/api/admin/auth/login")));
        assertFalse(matcher.matches(request("GET", "/api/admin/dashboard")));
        assertFalse(matcher.matches(request("OPTIONS", "/api/admin/packages")));
    }

    @Test
    void skipsCsrfWhenPathCannotBeResolved() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getServletPath()).thenReturn(null);
        when(request.getRequestURI()).thenReturn(null);

        assertFalse(matcher.matches(request));
    }

    @Test
    void skipsCsrfForPublicAndWebhookPosts() {
        assertFalse(matcher.matches(request("POST", "/api/payments/webhook")));
        assertFalse(matcher.matches(request("POST", "/api/bookings")));
        assertFalse(matcher.matches(request("POST", "/api/contact/enquiry")));
        assertFalse(matcher.matches(request("GET", "/api/health")));
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
