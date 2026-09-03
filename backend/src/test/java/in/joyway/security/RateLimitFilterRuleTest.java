package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterRuleTest {

    private final RateLimitProperties properties = new RateLimitProperties();
    private final RateLimitFilter filter = new RateLimitFilter(
            properties,
            new ObjectMapper(),
            new UrlBasedCorsConfigurationSource(),
            new InMemoryRateLimitBackend(java.time.Duration.ofMinutes(1)),
            new ClientAddressResolver()
    );

    @Test
    void mapsEndpointsToExpectedBucketsAndLimits() {
        assertRule("POST", RateLimitFilter.LOGIN_PATH, RateLimitBucket.LOGIN, 10);
        assertRule("POST", RateLimitFilter.BOOKINGS_PATH, RateLimitBucket.BOOKING_CREATE, 20);
        assertRule("POST", "/api/bookings/lookup", RateLimitBucket.BOOKING_ACCESS, 10);
        assertRule("GET", "/api/bookings/JW-1", RateLimitBucket.BOOKING_ACCESS, 10);
        assertRule("POST", RateLimitFilter.PAYMENT_ORDERS_PATH, RateLimitBucket.PAYMENT, 30);
        assertRule("POST", RateLimitFilter.PAYMENT_VERIFY_PATH, RateLimitBucket.PAYMENT, 30);
        assertRule("POST", RateLimitFilter.PAYMENT_ALIAS_ORDER_PATH, RateLimitBucket.PAYMENT, 30);
        assertRule("POST", RateLimitFilter.PAYMENT_ALIAS_VERIFY_PATH, RateLimitBucket.PAYMENT, 30);
        assertRule("POST", RateLimitFilter.CONTACT_ENQUIRY_PATH, RateLimitBucket.CONTACT, 5);
    }

    @Test
    void doesNotRateLimitWebhookOrUnmatchedPaths() {
        assertThat(filter.ruleFor(request("POST", RateLimitFilter.WEBHOOK_PATH))).isNull();
        assertThat(filter.ruleFor(request("GET", "/api/health"))).isNull();
    }

    @Test
    void optionsBookingPathStillHasRuleButFilterBypassesOptions() {
        // OPTIONS bypass happens in doFilterInternal before tryAcquire; see RateLimitFilterCorsTest.
        assertThat(filter.ruleFor(request("OPTIONS", "/api/bookings/lookup"))).isNotNull();
    }

    private void assertRule(String method, String path, RateLimitBucket bucket, int limit) {
        RateLimitFilter.RateLimitRule rule = filter.ruleFor(request(method, path));
        assertThat(rule).isNotNull();
        assertThat(rule.bucket()).isEqualTo(bucket);
        assertThat(rule.limit()).isEqualTo(limit);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }
}
