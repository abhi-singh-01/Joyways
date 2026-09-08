package in.joyway.exception;

import org.springframework.http.HttpStatus;

public class RazorpayApiException extends RuntimeException {

    private final HttpStatus status;

    public RazorpayApiException(String message, int statusCode) {
        super(message);
        this.status = HttpStatus.valueOf(statusCode);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
