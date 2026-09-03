package in.joyway.util;

/**
 * Spring Framework 6.2+ {@code ResponseCookie} rejects domains with a leading dot (e.g.
 * {@code .joyway.in}). Browsers accept {@code Domain=joyway.in} for the registrable domain and
 * its subdomains, so legacy configs may still use the dotted form.
 */
public final class CookieDomainNormalizer {

    private CookieDomainNormalizer() {
    }

    public static String forResponseCookie(String configuredDomain) {
        if (configuredDomain == null || configuredDomain.isBlank()) {
            return null;
        }
        String trimmed = configuredDomain.trim();
        if (trimmed.startsWith(".")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }
}
