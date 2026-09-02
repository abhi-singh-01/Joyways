package in.joyway.config;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.MappingSocketAddressResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Maps OSS cluster node IP addresses advertised by Azure Managed Redis back to the
 * configured {@code REDIS_HOST} hostname so TLS certificate SAN validation succeeds.
 *
 * <p>Azure Redis TLS certificates contain the hostname, not shard IP addresses. Lettuce
 * cluster topology discovery connects to nodes by IP on dynamic 85xx ports; without this
 * mapping, Java SSL fails with {@code No subject alternative names matching IP address}.
 *
 * @see <a href="https://github.com/Azure/AzureCacheForRedis/blob/main/Lettuce%20Best%20Practices.md">Azure Lettuce Best Practices</a>
 */
final class AzureRedisClusterAddressMapping {

    private AzureRedisClusterAddressMapping() {
    }

    static ClientResources clientResources(String redisHost) {
        return DefaultClientResources.builder()
                .socketAddressResolver(createResolver(redisHost))
                .build();
    }

    static MappingSocketAddressResolver createResolver(String redisHost) {
        String normalizedHost = normalizeHost(redisHost);
        return MappingSocketAddressResolver.create(
                DnsResolvers.JVM_DEFAULT,
                discovered -> mapClusterNodeAddress(normalizedHost, discovered, resolveHostAddresses(normalizedHost))
        );
    }

    /**
     * When {@code discovered} targets a resolved {@code redisHost} IP, return the hostname
     * with the same port so TLS peer verification uses the certificate SAN.
     */
    static HostAndPort mapClusterNodeAddress(String redisHost, HostAndPort discovered, Set<String> redisHostIps) {
        if (redisHost == null || redisHost.isBlank() || redisHostIps.isEmpty()) {
            return discovered;
        }
        if (redisHostIps.contains(discovered.getHostText())) {
            return HostAndPort.of(redisHost, discovered.getPort());
        }
        return discovered;
    }

    static Set<String> resolveHostAddresses(String redisHost) {
        if (redisHost == null || redisHost.isBlank()) {
            return Collections.emptySet();
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(redisHost.trim());
            Set<String> ips = LinkedHashSet.newLinkedHashSet(addresses.length);
            for (InetAddress address : addresses) {
                ips.add(address.getHostAddress());
            }
            return ips;
        } catch (UnknownHostException ex) {
            return Collections.emptySet();
        }
    }

    private static String normalizeHost(String redisHost) {
        return redisHost == null ? "" : redisHost.trim();
    }
}
