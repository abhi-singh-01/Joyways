package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import in.joyway.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Throttles endpoints that are attractive to abuse: admin login (credential
 * stuffing), booking creation (spam bookings and unauthenticated file uploads) and
 * payment order creation.
 *
 * <p>The Razorpay webhook is deliberately <em>not</em> throttled. Dropping a webhook
 * would leave a captured payment without a confirmed booking, and the endpoint is
 * already protected by HMAC signature verification plus event-id idempotency.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
@SuppressWarnings("java:S1075")
public class RateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = "/api/admin/auth/login";
    static final String BOOKINGS_PATH = "/api/bookings";
    static final String BOOKINGS_PREFIX = "/api/bookings/";
    static final String PAYMENT_ORDERS_PATH = "/api/payments/orders";
    static final String PAYMENT_VERIFY_PATH = "/api/payments/verify";
    static final String PAYMENT_ALIAS_ORDER_PATH = "/api/create-order";
    static final String PAYMENT_ALIAS_VERIFY_PATH = "/api/verify-payment";
    static final String WEBHOOK_PATH = "/api/payments/webhook";
    static final String CONTACT_ENQUIRY_PATH = "/api/contact/enquiry";
    static final String WAITLIST_PATH_PREFIX = "/api/packages/";
    static final String WAITLIST_PATH_SUFFIX = "/waitlist";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RateLimitBackend rateLimitBackend;
    private final ClientAddressResolver clientAddressResolver;

    public RateLimitFilter(RateLimitProperties properties,
                           ObjectMapper objectMapper,
                           CorsConfigurationSource corsConfigurationSource,
                           RateLimitBackend rateLimitBackend,
                           ClientAddressResolver clientAddressResolver) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.corsConfigurationSource = corsConfigurationSource;
        this.rateLimitBackend = rateLimitBackend;
        this.clientAddressResolver = clientAddressResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = ruleFor(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = clientAddressResolver.resolve(request);
        RateLimitResult result = rateLimitBackend.tryAcquire(
                rule.bucket(),
                clientKey,
                rule.limit(),
                Instant.now()
        );

        if (result.outcome() == RateLimitOutcome.REJECTED) {
            log.warn("event=security.rate_limit_exceeded path={} bucket={} client={}",
                    request.getRequestURI(), rule.bucket().id(), ClientAddressResolver.maskForLog(clientKey));
            writeTooManyRequests(request, response);
            return;
        }

        if (result.outcome() == RateLimitOutcome.SERVICE_UNAVAILABLE) {
            writeServiceUnavailable(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * @return the rule for this request, or {@code null} when the endpoint is not throttled.
     */
    RateLimitRule ruleFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (WEBHOOK_PATH.equals(path)) {
            return null;
        }

        if ("POST".equals(method) && LOGIN_PATH.equals(path)) {
            return new RateLimitRule(RateLimitBucket.LOGIN, properties.getLoginRequestsPerMinute());
        }
        if ("POST".equals(method) && BOOKINGS_PATH.equals(path)) {
            return new RateLimitRule(RateLimitBucket.BOOKING_CREATE, properties.getBookingRequestsPerMinute());
        }
        if (path.startsWith(BOOKINGS_PREFIX)) {
            return new RateLimitRule(RateLimitBucket.BOOKING_ACCESS, properties.getBookingLookupRequestsPerMinute());
        }
        if ("POST".equals(method) && (PAYMENT_ORDERS_PATH.equals(path)
                || PAYMENT_VERIFY_PATH.equals(path)
                || PAYMENT_ALIAS_ORDER_PATH.equals(path)
                || PAYMENT_ALIAS_VERIFY_PATH.equals(path))) {
            return new RateLimitRule(RateLimitBucket.PAYMENT, properties.getPaymentRequestsPerMinute());
        }
        if ("POST".equals(method) && CONTACT_ENQUIRY_PATH.equals(path)) {
            return new RateLimitRule(RateLimitBucket.CONTACT, properties.getContactRequestsPerMinute());
        }
        if ("POST".equals(method) && path.startsWith(WAITLIST_PATH_PREFIX) && path.endsWith(WAITLIST_PATH_SUFFIX)) {
            return new RateLimitRule(RateLimitBucket.CONTACT, properties.getContactRequestsPerMinute());
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        applyCorsHeaders(request, response);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error("Too many requests. Please try again in a minute.", "RATE_LIMITED")
        );
    }

    private void writeServiceUnavailable(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        applyCorsHeaders(request, response);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error("Rate limiting temporarily unavailable. Please try again shortly.",
                        "RATE_LIMIT_UNAVAILABLE")
        );
    }

    /**
     * This filter short-circuits ahead of Spring Security's CORS filter, so a throttled
     * browser request would otherwise receive a response without CORS headers. The browser
     * then discards it as a CORS failure and the caller sees a generic network error instead
     * of the rate-limit message.
     */
    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);
        if (configuration == null) {
            return;
        }

        String allowedOrigin = configuration.checkOrigin(origin);
        if (allowedOrigin == null) {
            return;
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        if (Boolean.TRUE.equals(configuration.getAllowCredentials())) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    }

    @Scheduled(fixedDelay = 300_000)
    void evictExpiredWindows() {
        rateLimitBackend.evictExpired(Instant.now());
    }

    record RateLimitRule(RateLimitBucket bucket, int limit) {
    }
}
