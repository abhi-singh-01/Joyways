package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import in.joyway.support.RedisClusterTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests against a real OSS Redis Cluster topology (Testcontainers).
 * Required pre-deployment gate for {@code RATE_LIMIT_BACKEND=redis}.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisClusterRateLimitIntegrationTest {

    private static final int MASTER_COUNT = 3;

    private static RedisClusterTestSupport cluster;

    @BeforeAll
    static void startCluster() {
        cluster = RedisClusterTestSupport.start();
    }

    @AfterAll
    static void stopCluster() {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    void connectsAndDiscoversClusterTopology() {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-conn");
        LettuceConnectionFactory factory = cluster.createConnectionFactory(properties);

        assertThat(factory.getClusterConfiguration()).isNotNull();

        try (RedisClusterConnection connection = factory.getClusterConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
            assertThat(connection.clusterGetNodes()).hasSizeGreaterThanOrEqualTo(MASTER_COUNT);
        }
    }

    @Test
    void incrAndExpireWorkOnCluster() {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-cmd");
        StringRedisTemplate template = cluster.createTemplate(properties);

        Long first = template.opsForValue().increment("joyway:rl:cluster-cmd:incr-key");
        template.expire("joyway:rl:cluster-cmd:incr-key", Duration.ofSeconds(60));

        assertThat(first).isEqualTo(1L);
        assertThat(template.getExpire("joyway:rl:cluster-cmd:incr-key")).isPositive();
    }

    @Test
    void luaRateLimitScriptWorksOnCluster() {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-lua");
        RedisRateLimitBackend backend = backend(properties);
        Instant now = Instant.parse("2026-09-02T13:00:00Z");

        for (int i = 0; i < 10; i++) {
            assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.10", 10, now).isAllowed()).isTrue();
        }
        assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.10", 10, now).isAllowed()).isFalse();
    }

    @Test
    void keepsIndependentKeysOnCluster() {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-keys");
        RedisRateLimitBackend backend = backend(properties);
        Instant now = Instant.parse("2026-09-02T13:01:00Z");

        for (int i = 0; i < 10; i++) {
            backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.20", 10, now);
        }

        assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.21", 10, now).isAllowed()).isTrue();
        assertThat(backend.tryAcquire(RateLimitBucket.BOOKING_CREATE, "203.0.113.20", 10, now).isAllowed()).isTrue();
    }

    @Test
    void concurrentIncrementsRemainWithinLimitOnCluster() throws Exception {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-concurrent");
        RedisRateLimitBackend backend = backend(properties);
        Instant now = Instant.parse("2026-09-02T13:02:00Z");
        int limit = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<RateLimitResult>> futures = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> backend.tryAcquire(RateLimitBucket.PAYMENT, "203.0.113.50", limit, now),
                        executor
                ));
            }
            long allowed = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(RateLimitResult::isAllowed)
                    .count();
            assertThat(allowed).isEqualTo(limit);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void twoBackendsShareClusterCounter() {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-shared");
        properties.setLoginRequestsPerMinute(10);

        StringRedisTemplate template = cluster.createTemplate(properties);
        RedisRateLimitBackend sharedRedis = new RedisRateLimitBackend(template, properties);
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(Duration.ofMinutes(1));
        ResilientRateLimitBackend backendA = new ResilientRateLimitBackend(sharedRedis, emergency, properties);
        ResilientRateLimitBackend backendB = new ResilientRateLimitBackend(sharedRedis, emergency, properties);

        Instant now = Instant.parse("2026-09-02T13:03:00Z");
        for (int i = 0; i < 5; i++) {
            assertThat(backendA.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.40", 10, now).isAllowed()).isTrue();
            assertThat(backendB.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.40", 10, now).isAllowed()).isTrue();
        }
        assertThat(backendA.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.40", 10, now).isAllowed()).isFalse();
        assertThat(backendB.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.40", 10, now).isAllowed()).isFalse();
    }

    @Test
    void healthyClusterDoesNotEnterDegradedModeForLogin() {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-degraded");
        properties.setLoginFailClosedOnRedisError(true);

        StringRedisTemplate template = cluster.createTemplate(properties);
        RedisRateLimitBackend sharedRedis = new RedisRateLimitBackend(template, properties);
        ResilientRateLimitBackend backend = new ResilientRateLimitBackend(
                sharedRedis,
                new InMemoryRateLimitBackend(Duration.ofMinutes(1)),
                properties
        );

        RateLimitResult result = backend.tryAcquire(
                RateLimitBucket.LOGIN,
                "203.0.113.60",
                10,
                Instant.parse("2026-09-02T13:04:00Z")
        );

        assertThat(result.outcome()).isNotEqualTo(RateLimitOutcome.SERVICE_UNAVAILABLE);
        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void twoFiltersShareClusterCounterForLogin() throws Exception {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:cluster-filter");
        properties.setLoginRequestsPerMinute(10);

        StringRedisTemplate template = cluster.createTemplate(properties);
        RedisRateLimitBackend sharedRedis = new RedisRateLimitBackend(template, properties);
        ResilientRateLimitBackend sharedBackend = new ResilientRateLimitBackend(
                sharedRedis,
                new InMemoryRateLimitBackend(Duration.ofMinutes(1)),
                properties
        );

        RateLimitFilter filterA = buildFilter(properties, sharedBackend);
        RateLimitFilter filterB = buildFilter(properties, sharedBackend);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, login(filterA, "203.0.113.70").getStatus());
            assertEquals(200, login(filterB, "203.0.113.70").getStatus());
        }

        assertEquals(429, login(filterA, "203.0.113.70").getStatus());
        assertEquals(429, login(filterB, "203.0.113.70").getStatus());
    }

    private static RedisRateLimitBackend backend(RateLimitProperties properties) {
        return new RedisRateLimitBackend(cluster.createTemplate(properties), properties);
    }

    private static RateLimitFilter buildFilter(RateLimitProperties properties, RateLimitBackend backend) {
        return new RateLimitFilter(
                properties,
                new ObjectMapper(),
                new UrlBasedCorsConfigurationSource(),
                backend,
                new ClientAddressResolver()
        );
    }

    private static MockHttpServletResponse login(RateLimitFilter filter, String clientIp) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", RateLimitFilter.LOGIN_PATH);
        request.setRequestURI(RateLimitFilter.LOGIN_PATH);
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
