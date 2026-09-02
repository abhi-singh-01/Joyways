package in.joyway.config;

import in.joyway.security.ClientAddressResolver;
import in.joyway.security.InMemoryRateLimitBackend;
import in.joyway.security.RateLimitBackend;
import in.joyway.security.RedisRateLimitBackend;
import in.joyway.security.ResilientRateLimitBackend;
import io.lettuce.core.SslOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
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

    /**
     * Azure Managed Redis uses {@code OSSCluster} topology. Clients must connect to the
     * public endpoint (host:10000) with a cluster-aware Lettuce factory so shard discovery
     * can reach dynamic 85xx node ports.
     */
    @Bean
    @ConditionalOnProperty(prefix = "joyway.rate-limit", name = "backend", havingValue = "redis")
    LettuceConnectionFactory rateLimitRedisConnectionFactory(RateLimitProperties properties) {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        clusterConfig.clusterNode(new RedisNode(properties.getRedisHost(), properties.getRedisPort()));
        if (StringUtils.hasText(properties.getRedisPassword())) {
            clusterConfig.setPassword(RedisPassword.of(properties.getRedisPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofMillis(properties.getRedisCommandTimeoutMs()));

        ClusterClientOptions.Builder clusterOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
                        .enableAllAdaptiveRefreshTriggers()
                        .build());

        if (properties.isRedisSsl()) {
            clientBuilder.useSsl();
            clusterOptions.sslOptions(SslOptions.builder().build());
        }

        clientBuilder.clientOptions(clusterOptions.build());

        return new LettuceConnectionFactory(clusterConfig, clientBuilder.build());
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
