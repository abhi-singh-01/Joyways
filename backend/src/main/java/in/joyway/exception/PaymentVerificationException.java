package in.joyway.exception;

import org.springframework.http.HttpStatus;

public class PaymentVerificationException extends RuntimeException {

    private final HttpStatus status;

    public PaymentVerificationException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
