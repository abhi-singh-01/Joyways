package in.joyway.config;

import in.joyway.security.ClientAddressResolver;
import in.joyway.security.InMemoryRateLimitBackend;
import in.joyway.security.RateLimitBackend;
import in.joyway.security.RedisRateLimitBackend;
import in.joyway.security.ResilientRateLimitBackend;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
public class RateLimitBackendConfig {

    @Bean
    ClientAddressResolver clientAddressResolver() {
        return new ClientAddressResolver();
    }

    private static InMemoryRateLimitBackend newEmergencyBackend() {
        return new InMemoryRateLimitBackend(Duration.ofMinutes(1));
    }

    @Bean
    @ConditionalOnProperty(prefix = "joyway.rate-limit", name = "backend", havingValue = "memory", matchIfMissing = true)
    RateLimitBackend inMemoryRateLimitBackend() {
        return newEmergencyBackend();
    }

    @Bean
    @ConditionalOnProperty(prefix = "joyway.rate-limit", name = "backend", havingValue = "redis")
    LettuceConnectionFactory rateLimitRedisConnectionFactory(RateLimitProperties properties) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getRedisHost());
        redisConfig.setPort(properties.getRedisPort());
        if (StringUtils.hasText(properties.getRedisPassword())) {
            redisConfig.setPassword(properties.getRedisPassword());
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofMillis(properties.getRedisCommandTimeoutMs()));

        if (properties.isRedisSsl()) {
            clientBuilder.useSsl();
            clientBuilder.clientOptions(ClientOptions.builder()
                    .sslOptions(SslOptions.builder().build())
                    .build());
        }

        return new LettuceConnectionFactory(redisConfig, clientBuilder.build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "joyway.rate-limit", name = "backend", havingValue = "redis")
    StringRedisTemplate rateLimitRedisTemplate(LettuceConnectionFactory rateLimitRedisConnectionFactory) {
        return new StringRedisTemplate(rateLimitRedisConnectionFactory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "joyway.rate-limit", name = "backend", havingValue = "redis")
    RateLimitBackend redisRateLimitBackend(StringRedisTemplate rateLimitRedisTemplate,
                                           RateLimitProperties properties) {
        RedisRateLimitBackend redisBackend = new RedisRateLimitBackend(rateLimitRedisTemplate, properties);
        return new ResilientRateLimitBackend(redisBackend, newEmergencyBackend(), properties);
    }
}
