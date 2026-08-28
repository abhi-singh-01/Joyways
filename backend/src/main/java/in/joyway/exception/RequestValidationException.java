package in.joyway.exception;

import java.util.Map;

/**
 * Carries per-field validation failures for requests that are validated
 * programmatically rather than through {@code @Valid} method arguments,
 * such as the JSON part of a multipart booking submission.
 */
public class RequestValidationException extends RuntimeException {

    private final transient Map<String, String> fieldErrors;

    public RequestValidationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
