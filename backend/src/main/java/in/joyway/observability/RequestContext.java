package in.joyway.observability;

import org.slf4j.MDC;

/**
 * Read-only access to the current request's correlation id.
 */
public final class RequestContext {

    private RequestContext() {
    }

    /**
     * @return the correlation id for the in-flight request, or {@code null} outside a request
     *         (for example on scheduler threads).
     */
    public static String currentRequestId() {
        return MDC.get(RequestCorrelationFilter.MDC_KEY);
    }
}
