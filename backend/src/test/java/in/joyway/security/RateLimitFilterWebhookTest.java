package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitFilterWebhookTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setPaymentRequestsPerMinute(1);

        InMemoryRateLimitBackend backend = new InMemoryRateLimitBackend(java.time.Duration.ofMinutes(1));
        filter = new RateLimitFilter(
                properties,
                new ObjectMapper(),
                new UrlBasedCorsConfigurationSource(),
                backend,
                new ClientAddressResolver()
        );
    }

    @Test
    void webhookIsNeverRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", RateLimitFilter.WEBHOOK_PATH);
            request.setRequestURI(RateLimitFilter.WEBHOOK_PATH);
            request.setRemoteAddr("203.0.113.99");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void webhookRuleResolutionReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", RateLimitFilter.WEBHOOK_PATH);
        request.setRequestURI(RateLimitFilter.WEBHOOK_PATH);
        assertNull(filter.ruleFor(request));
    }
}
