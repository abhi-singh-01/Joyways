package in.joyway.exception;

import org.springframework.http.HttpStatus;

public class OperationConflictException extends RuntimeException {

    private static final HttpStatus STATUS = HttpStatus.CONFLICT;

    public OperationConflictException(String message) {
        super(message);
    }

    public HttpStatus getStatus() {
        return STATUS;
    }
}
