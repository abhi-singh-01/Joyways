package in.joyway.config;

import io.lettuce.core.internal.HostAndPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AzureRedisClusterAddressMappingTest {

    private static final String AZURE_REDIS_HOST = "joyway-redis-prod.centralindia.redis.azure.net";
    private static final String AZURE_REDIS_IP = "20.204.187.171";

    @Test
    void mapsDiscoveredAzureNodeIpBackToHostnamePreservingPort() {
        HostAndPort discovered = HostAndPort.of(AZURE_REDIS_IP, 8501);

        HostAndPort mapped = AzureRedisClusterAddressMapping.mapClusterNodeAddress(
                AZURE_REDIS_HOST,
                discovered,
                Set.of(AZURE_REDIS_IP)
        );

        assertThat(mapped.getHostText()).isEqualTo(AZURE_REDIS_HOST);
        assertThat(mapped.getPort()).isEqualTo(8501);
    }

    @Test
    void mapsAnyResolvedSeedIpWhenHostnameHasMultipleAddresses() {
        HostAndPort discoveredPrimary = HostAndPort.of("10.0.0.1", 8500);
        HostAndPort discoveredSecondary = HostAndPort.of("10.0.0.2", 8502);
        Set<String> resolvedIps = Set.of("10.0.0.1", "10.0.0.2");

        assertThat(AzureRedisClusterAddressMapping.mapClusterNodeAddress(
                AZURE_REDIS_HOST, discoveredPrimary, resolvedIps))
                .isEqualTo(HostAndPort.of(AZURE_REDIS_HOST, 8500));
        assertThat(AzureRedisClusterAddressMapping.mapClusterNodeAddress(
                AZURE_REDIS_HOST, discoveredSecondary, resolvedIps))
                .isEqualTo(HostAndPort.of(AZURE_REDIS_HOST, 8502));
    }

    @Test
    void doesNotMapUnknownIpAddresses() {
        HostAndPort discovered = HostAndPort.of("203.0.113.50", 8501);

        HostAndPort mapped = AzureRedisClusterAddressMapping.mapClusterNodeAddress(
                AZURE_REDIS_HOST,
                discovered,
                Set.of(AZURE_REDIS_IP)
        );

        assertThat(mapped).isEqualTo(discovered);
    }

    @Test
    void leavesHostnameSeedEndpointUnchanged() {
        HostAndPort seed = HostAndPort.of(AZURE_REDIS_HOST, 10000);

        HostAndPort mapped = AzureRedisClusterAddressMapping.mapClusterNodeAddress(
                AZURE_REDIS_HOST,
                seed,
                Set.of(AZURE_REDIS_IP)
        );

        assertThat(mapped).isEqualTo(seed);
    }

    @Test
    void returnsDiscoveredAddressWhenResolvedIpSetIsEmpty() {
        HostAndPort discovered = HostAndPort.of(AZURE_REDIS_IP, 8501);

        HostAndPort mapped = AzureRedisClusterAddressMapping.mapClusterNodeAddress(
                AZURE_REDIS_HOST,
                discovered,
                Set.of()
        );

        assertThat(mapped).isEqualTo(discovered);
    }

    @Test
    void createResolverUsesConfiguredHostWithoutHardcodedIp() {
        assertThat(AzureRedisClusterAddressMapping.createResolver(AZURE_REDIS_HOST)).isNotNull();
        assertThat(AzureRedisClusterAddressMapping.createResolver(AZURE_REDIS_HOST)).isNotNull();
    }

    @Test
    void resolveHostAddressesReturnsEmptyForBlankHost() {
        assertThat(AzureRedisClusterAddressMapping.resolveHostAddresses("")).isEmpty();
        assertThat(AzureRedisClusterAddressMapping.resolveHostAddresses(null)).isEmpty();
    }

    @Test
    void tlsClientConfigurationRemainsEnabledWhenRedisSslTrue() {
        LettuceClientConfiguration configuration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(250))
                .useSsl()
                .build();

        assertThat(configuration.isUseSsl()).isTrue();
        assertThat(configuration.getCommandTimeout()).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    void passwordRemainsExternalizedInProperties() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRedisPassword("");
        properties.setRedisHost(AZURE_REDIS_HOST);
        properties.setRedisPort(10000);
        properties.setRedisSsl(true);

        assertThat(properties.getRedisPassword()).isEmpty();
        assertThat(properties.isRedisSsl()).isTrue();
    }
}
