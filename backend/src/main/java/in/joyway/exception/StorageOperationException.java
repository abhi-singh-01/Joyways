package in.joyway.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a storage operation fails after validation and authorization.
 */
public class StorageOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public StorageOperationException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public StorageOperationException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
