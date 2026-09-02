package in.joyway.security;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves the client address for rate limiting behind Azure Container Apps ingress.
 *
 * <p>Uses the right-most non-empty {@code X-Forwarded-For} hop because ingress proxies append
 * the connecting client. The left-most values may be spoofed by the original client and must
 * not be trusted when ingress is in the path.
 *
 * <p><strong>Limitation:</strong> If the application is reachable directly (bypassing ingress),
 * a client can supply an arbitrary {@code X-Forwarded-For}. Production must expose only the
 * Container Apps ingress endpoint, not individual replica addresses.
 */
public class ClientAddressResolver {

    static final String UNKNOWN_CLIENT = "unknown";

    public String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (!hop.isEmpty()) {
                    return normalize(hop);
                }
            }
        }
        String remote = request.getRemoteAddr();
        if (remote != null && !remote.isBlank()) {
            return normalize(remote);
        }
        return UNKNOWN_CLIENT;
    }

    /**
     * Canonicalizes IPv4/IPv6 where possible. Malformed values are returned trimmed unchanged
     * so they still form a stable (if coarse) rate-limit key.
     */
    public String normalize(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return UNKNOWN_CLIENT;
        }
        String trimmed = rawAddress.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        try {
            return InetAddress.getByName(trimmed).getHostAddress();
        } catch (UnknownHostException ex) {
            return trimmed;
        }
    }

    /** Mask for logs — keeps prefix only. */
    public static String maskForLog(String client) {
        if (client == null || UNKNOWN_CLIENT.equals(client)) {
            return UNKNOWN_CLIENT;
        }
        int lastSeparator = Math.max(client.lastIndexOf('.'), client.lastIndexOf(':'));
        return lastSeparator > 0 ? client.substring(0, lastSeparator) + ".x" : UNKNOWN_CLIENT;
    }
}
