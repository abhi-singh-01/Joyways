package in.joyway.util;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Expands a comma-separated CORS origin list so apex and {@code www} variants both work.
 * {@code https://joyway.in} and {@code https://www.joyway.in} are the same site; browsers send
 * different {@code Origin} values until the www host is redirected.
 */
public final class CorsOriginExpander {

    private CorsOriginExpander() {
    }

    public static List<String> expand(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return List.of();
        }
        Set<String> origins = new LinkedHashSet<>();
        Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(origin -> {
                    origins.add(origin);
                    String twin = wwwTwin(origin);
                    if (twin != null) {
                        origins.add(twin);
                    }
                });
        return List.copyOf(origins);
    }

    static String wwwTwin(String origin) {
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String host = uri.getHost();
        if (host == null) {
            return null;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(lower) || lower.endsWith(".localhost") || lower.startsWith("127.")) {
            return null;
        }
        String[] labels = lower.split("\\.");
        String twinHost;
        if (labels.length == 2) {
            twinHost = "www." + lower;
        } else if (labels.length == 3 && "www".equals(labels[0])) {
            twinHost = lower.substring(4);
        } else {
            return null;
        }
        int port = uri.getPort();
        String portPart = port == -1 ? "" : ":" + port;
        String scheme = uri.getScheme();
        if (scheme == null) {
            return null;
        }
        return scheme + "://" + twinHost + portPart;
    }
}
