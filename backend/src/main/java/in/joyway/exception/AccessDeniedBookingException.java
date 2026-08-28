package in.joyway.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedBookingException extends RuntimeException {

    private static final String GENERIC_MESSAGE = "Booking not found.";

    private final HttpStatus status;

    public AccessDeniedBookingException(String message) {
        super(GENERIC_MESSAGE);
        this.status = HttpStatus.NOT_FOUND;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
