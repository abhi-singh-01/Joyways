package in.joyway.security;

import in.joyway.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimitBackendTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

    private RedisRateLimitBackend backend() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRedisKeyPrefix("joyway:rl:test");
        properties.setWindowTtlSeconds(120);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(),
                redis.getMappedPort(6379)
        );
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return new RedisRateLimitBackend(template, properties);
    }

    @Test
    void allowsRequestsUpToLimit() {
        RedisRateLimitBackend backend = backend();
        Instant now = Instant.parse("2026-09-02T12:00:00Z");

        for (int i = 0; i < 10; i++) {
            assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.1", 10, now).isAllowed()).isTrue();
        }
        assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.1", 10, now).isAllowed()).isFalse();
    }

    @Test
    void keepsSeparateClientsIndependent() {
        RedisRateLimitBackend backend = backend();
        Instant now = Instant.parse("2026-09-02T12:01:00Z");

        for (int i = 0; i < 10; i++) {
            backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.1", 10, now);
        }

        assertThat(backend.tryAcquire(RateLimitBucket.LOGIN, "203.0.113.2", 10, now).isAllowed()).isTrue();
    }

    @Test
    void concurrentIncrementsRemainWithinLimit() throws Exception {
        RedisRateLimitBackend backend = backend();
        Instant now = Instant.parse("2026-09-02T12:02:00Z");
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
}
