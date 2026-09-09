package in.joyway.support;

import in.joyway.config.RateLimitProperties;
import io.lettuce.core.SslOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spins up a real 3-master OSS Redis Cluster locally for integration tests.
 * Uses NAT port mapping so Lettuce can follow cluster topology announcements.
 *
 * <p>Does not connect to production Azure Redis.
 */
public final class RedisClusterTestSupport implements AutoCloseable {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final int MASTER_COUNT = 3;
    private static final int REDIS_PORT = 6379;

    private final Network network;
    private final List<GenericContainer<?>> masters;
    private final GenericContainer<?> clusterCreator;
    private final String dockerHost;
    private final ClientResources clientResources;

    private RedisClusterTestSupport(
            Network network,
            List<GenericContainer<?>> masters,
            GenericContainer<?> clusterCreator,
            Map<String, Integer> containerAddressToMappedPort
    ) {
        this.network = network;
        this.masters = masters;
        this.clusterCreator = clusterCreator;
        this.dockerHost = masters.getFirst().getHost();
        this.clientResources = DefaultClientResources.builder()
                .socketAddressResolver(MappingSocketAddressResolver.create(
                        (HostAndPort hostAndPort) -> {
                            Integer mappedPort = containerAddressToMappedPort.get(hostAndPort.getHostText());
                            if (mappedPort != null && hostAndPort.getPort() == REDIS_PORT) {
                                return HostAndPort.of(dockerHost, mappedPort);
                            }
                            return hostAndPort;
                        }
                ))
                .build();
    }

    public static RedisClusterTestSupport start() {
        Network network = Network.newNetwork();
        List<GenericContainer<?>> masters = new ArrayList<>();
        Map<String, Integer> containerAddressToMappedPort = new LinkedHashMap<>();

        for (int index = 0; index < MASTER_COUNT; index++) {
            GenericContainer<?> master = new GenericContainer<>(REDIS_IMAGE)
                    .withNetwork(network)
                    .withNetworkAliases("redis-master-" + index)
                    .withExposedPorts(REDIS_PORT)
                    .withCommand(
                            "redis-server",
                            "--port", String.valueOf(REDIS_PORT),
                            "--cluster-enabled", "yes",
                            "--cluster-config-file", "nodes.conf",
                            "--cluster-node-timeout", "5000",
                            "--appendonly", "yes",
                            "--bind", "0.0.0.0"
                    )
                    .waitingFor(Wait.forListeningPort());
            master.start();
            masters.add(master);
            String containerIp = master.getContainerInfo().getNetworkSettings().getNetworks().values()
                    .iterator().next().getIpAddress();
            containerAddressToMappedPort.put(containerIp, master.getMappedPort(REDIS_PORT));
        }

        String createCommand = masters.stream()
                .map(master -> master.getNetworkAliases().getFirst() + ":" + REDIS_PORT)
                .collect(Collectors.joining(" "))
                + " --cluster-replicas 0 --cluster-yes";

        GenericContainer<?> clusterCreator = new GenericContainer<>(REDIS_IMAGE)
                .withNetwork(network)
                .withCommand("sh", "-c", "sleep 3 && redis-cli --cluster create " + createCommand)
                .waitingFor(Wait.forLogMessage(".*All 16384 slots covered.*", 1));
        clusterCreator.start();

        waitForClusterReady(masters.getFirst());

        return new RedisClusterTestSupport(network, masters, clusterCreator, containerAddressToMappedPort);
    }

    private static void waitForClusterReady(GenericContainer<?> master) {
        org.testcontainers.shaded.org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> clusterStateOk(master));
    }

    private static boolean clusterStateOk(GenericContainer<?> master) {
        try {
            org.testcontainers.containers.Container.ExecResult result = master.execInContainer(
                    "redis-cli", "-p", String.valueOf(REDIS_PORT), "cluster", "info"
            );
            return result.getStdout().contains("cluster_state:ok");
        } catch (Exception ignored) {
            return false;
        }
    }

    public int seedPort() {
        return masters.getFirst().getMappedPort(REDIS_PORT);
    }

    public String host() {
        return dockerHost;
    }

    /**
     * Builds a cluster connection factory using the same configuration shape as
     * {@link in.joyway.config.RateLimitBackendConfig}.
     */
    public LettuceConnectionFactory createConnectionFactory(RateLimitProperties properties) {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        for (GenericContainer<?> master : masters) {
            clusterConfig.clusterNode(new RedisNode(
                    properties.getRedisHost(),
                    master.getMappedPort(REDIS_PORT)
            ));
        }
        if (StringUtils.hasText(properties.getRedisPassword())) {
            clusterConfig.setPassword(RedisPassword.of(properties.getRedisPassword()));
        }

        ClusterClientOptions.Builder clusterOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
                        .enableAllAdaptiveRefreshTriggers()
                        .build());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder()
                        .clientResources(clientResources)
                        .commandTimeout(Duration.ofMillis(properties.getRedisCommandTimeoutMs()));

        if (properties.isRedisSsl()) {
            clientBuilder.useSsl();
            clusterOptions.sslOptions(SslOptions.builder().build());
        }

        clientBuilder.clientOptions(clusterOptions.build());

        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientBuilder.build());
        factory.afterPropertiesSet();
        return factory;
    }

    public StringRedisTemplate createTemplate(RateLimitProperties properties) {
        StringRedisTemplate template = new StringRedisTemplate(createConnectionFactory(properties));
        template.afterPropertiesSet();
        return template;
    }

    public RateLimitProperties clusterProperties(String keyPrefix) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRedisHost(host());
        properties.setRedisPort(seedPort());
        properties.setRedisSsl(false);
        properties.setRedisPassword("");
        properties.setRedisCommandTimeoutMs(5_000);
        properties.setRedisKeyPrefix(keyPrefix);
        properties.setWindowTtlSeconds(120);
        return properties;
    }

    @Override
    public void close() {
        clientResources.shutdown();
        clusterCreator.stop();
        for (GenericContainer<?> master : masters) {
            master.stop();
        }
        network.close();
    }
}
