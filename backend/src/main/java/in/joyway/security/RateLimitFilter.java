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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
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
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/admin/auth/login";
    private static final String BOOKINGS_PATH = "/api/bookings";
    private static final String PAYMENT_ORDERS_PATH = "/api/payments/orders";
    private static final String PAYMENT_VERIFY_PATH = "/api/payments/verify";
    private static final String PAYMENT_ALIAS_ORDER_PATH = "/api/create-order";
    private static final String PAYMENT_ALIAS_VERIFY_PATH = "/api/verify-payment";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter = new RateLimiter(Duration.ofMinutes(1));

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer limit = limitFor(request);
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRequestURI() + "|" + clientKey(request);
        if (!rateLimiter.tryAcquire(key, limit, Instant.now())) {
            log.warn("event=security.rate_limit_exceeded path={} client={}",
                    request.getRequestURI(), maskClient(clientKey(request)));
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * @return the per-minute limit for this request, or {@code null} when the endpoint
     *         is not throttled.
     */
    private Integer limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && LOGIN_PATH.equals(path)) {
            return properties.getLoginRequestsPerMinute();
        }
        if ("POST".equals(method) && BOOKINGS_PATH.equals(path)) {
            return properties.getBookingRequestsPerMinute();
        }
        if ("POST".equals(method) && (PAYMENT_ORDERS_PATH.equals(path)
                || PAYMENT_VERIFY_PATH.equals(path)
                || PAYMENT_ALIAS_ORDER_PATH.equals(path)
                || PAYMENT_ALIAS_VERIFY_PATH.equals(path))) {
            return properties.getPaymentRequestsPerMinute();
        }
        return null;
    }

    /**
     * Uses the left-most X-Forwarded-For entry when present, since Azure terminates TLS
     * upstream and the socket address would otherwise be the load balancer for everyone.
     */
    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            String trimmed = first.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    /** Keeps enough of the address to correlate abuse without logging a full client IP. */
    private static String maskClient(String client) {
        int lastSeparator = Math.max(client.lastIndexOf('.'), client.lastIndexOf(':'));
        return lastSeparator > 0 ? client.substring(0, lastSeparator) + ".x" : "unknown";
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error("Too many requests. Please try again in a minute.", "RATE_LIMITED")
        );
    }

    @Scheduled(fixedDelay = 300_000)
    void evictExpiredWindows() {
        rateLimiter.evictExpired(Instant.now());
    }
}
