package in.joyway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDeniedBookingExceptionTest {

    @Test
    void usesNotFoundStatusToPreventEnumeration() {
        AccessDeniedBookingException ex = new AccessDeniedBookingException("ignored detail");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Booking not found.");
    }
}
