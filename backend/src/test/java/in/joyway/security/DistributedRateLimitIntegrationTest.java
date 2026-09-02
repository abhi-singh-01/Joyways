package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import in.joyway.support.RedisClusterTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Distributed rate-limit behaviour verified against OSS Redis Cluster topology.
 */
@Testcontainers(disabledWithoutDocker = true)
class DistributedRateLimitIntegrationTest {

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
    void twoFiltersShareRedisCounterForLogin() throws Exception {
        RateLimitProperties properties = cluster.clusterProperties("joyway:rl:dist-test");
        properties.setLoginRequestsPerMinute(10);

        StringRedisTemplate template = cluster.createTemplate(properties);
        RedisRateLimitBackend sharedRedis = new RedisRateLimitBackend(template, properties);
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(Duration.ofMinutes(1));
        ResilientRateLimitBackend sharedBackend = new ResilientRateLimitBackend(sharedRedis, emergency, properties);

        RateLimitFilter filterA = buildFilter(properties, sharedBackend);
        RateLimitFilter filterB = buildFilter(properties, sharedBackend);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, login(filterA, "203.0.113.40").getStatus());
            assertEquals(200, login(filterB, "203.0.113.40").getStatus());
        }

        assertEquals(429, login(filterA, "203.0.113.40").getStatus());
        assertEquals(429, login(filterB, "203.0.113.40").getStatus());
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
