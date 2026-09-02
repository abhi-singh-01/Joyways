package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class DistributedRateLimitIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

    @Test
    void twoFiltersShareRedisCounterForLogin() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setLoginRequestsPerMinute(10);
        properties.setRedisKeyPrefix("joyway:rl:dist-test");
        properties.setWindowTtlSeconds(120);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(),
                redis.getMappedPort(6379)
        );
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();

        RedisRateLimitBackend sharedRedis = new RedisRateLimitBackend(template, properties);
        InMemoryRateLimitBackend emergency = new InMemoryRateLimitBackend(java.time.Duration.ofMinutes(1));
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
