package in.joyway.exception;

import org.springframework.http.HttpStatus;

public class OperationConflictException extends RuntimeException {

    private final HttpStatus status = HttpStatus.CONFLICT;

    public OperationConflictException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
